package com.finsphere.frontend.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.finance.LeaveType;
import com.finsphere.common.dto.finance.LoanRequest;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.FinanceClient;
import com.finsphere.frontend.config.FrontendProperties;
import com.finsphere.frontend.web.support.Formats;
import com.finsphere.frontend.web.support.MemberDirectory;
import com.finsphere.frontend.web.support.Notice;
import com.finsphere.frontend.web.support.Paging;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.finsphere.frontend.client.BackendResults.data;

/** Daily-collection loans: everyone's (administrators) or your own, and disbursing a new loan. */
@Controller
@RequestMapping("/loans")
public class LoanController {

    private static final String NEW = "loans/new";

    private final FinanceClient financeClient;

    private final MemberDirectory members;

    private final FrontendProperties properties;

    private final Formats formats;

    public LoanController(FinanceClient financeClient, MemberDirectory members, FrontendProperties properties,
            Formats formats) {
        this.financeClient = financeClient;
        this.members = members;
        this.properties = properties;
        this.formats = formats;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        AuthenticatedUser me = SecurityUtils.requireCurrentUser();
        model.addAttribute("nav", "loans");
        if (me.isAdmin()) {
            PageResponse<LoanResponse> loans = data(
                    this.financeClient.loans(Paging.requested(page), this.properties.pageSize()));
            String lastPage = Paging.pastTheEnd("/loans", page, loans);
            if (lastPage != null) {
                return lastPage;
            }
            model.addAttribute("loans", loans.content());
            model.addAttribute("page", loans);
        }
        else {
            List<LoanResponse> loans = data(this.financeClient.myLoans());
            model.addAttribute("loans", loans);
            model.addAttribute("page", null);
        }
        return "loans/list";
    }

    @GetMapping("/new")
    public String newLoan(@RequestParam(name = "member", required = false) @Nullable Long member, Model model) {
        LoanRequest suggestion = new LoanRequest(member, null, BigDecimal.ZERO, 100, this.formats.today(),
                LeaveType.SUNDAY);
        return renderForm(model, suggestion, Map.of(), null);
    }

    @PostMapping
    public String disburse(@ModelAttribute("form") @Nullable LoanRequest submitted, BindingResult binding,
            Model model, HttpServletResponse response) {
        LoanRequest form = (submitted != null) ? submitted : new LoanRequest(null, null, null, null, null, null);
        if (binding.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return renderForm(model, form, FormErrors.of(binding), null);
        }
        try {
            data(this.financeClient.disburse(form));
            return "redirect:/loans?notice=" + Notice.LOAN_DISBURSED.key();
        }
        catch (BackendException ex) {
            if (!ex.isRejection()) {
                throw ex;
            }
            response.setStatus(ex.getStatus());
            return renderForm(model, form, ex.getFieldErrors(), ex.userMessage());
        }
    }

    private String renderForm(Model model, LoanRequest form, Map<String, String> errors, @Nullable String error) {
        model.addAttribute("nav", "loans");
        model.addAttribute("form", form);
        model.addAttribute("members", this.members.all());
        model.addAttribute("leaveTypes", LeaveType.values());
        model.addAttribute("errors", errors);
        model.addAttribute("error", error);
        return NEW;
    }
}
