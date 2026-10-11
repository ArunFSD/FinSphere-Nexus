package com.finsphere.frontend.web;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.dto.finance.PaymentMode;
import com.finsphere.common.dto.finance.TransactionRequest;
import com.finsphere.common.dto.finance.TransactionResponse;
import com.finsphere.common.dto.finance.TransactionType;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.ChitClient;
import com.finsphere.frontend.client.FinanceClient;
import com.finsphere.frontend.config.FrontendProperties;
import com.finsphere.frontend.web.form.PaymentForm;
import com.finsphere.frontend.web.support.Formats;
import com.finsphere.frontend.web.support.Loaded;
import com.finsphere.frontend.web.support.MemberDirectory;
import com.finsphere.frontend.web.support.Notice;
import com.finsphere.frontend.web.support.Paging;
import com.finsphere.frontend.web.support.SectionLoader;
import com.finsphere.frontend.web.support.SectionLoader.Pending;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.finsphere.frontend.client.BackendResults.data;

/**
 * The payment log book. Recording a payment takes two steps: pick the member, then pick which of their schemes or
 * loans the money is for (the page lists only those).
 */
@Controller
@RequestMapping("/payments")
public class PaymentController {

    private static final String NEW = "payments/new";

    private final FinanceClient financeClient;

    private final ChitClient chitClient;

    private final SectionLoader sections;

    private final MemberDirectory members;

    private final FrontendProperties properties;

    private final Formats formats;

    public PaymentController(FinanceClient financeClient, ChitClient chitClient, SectionLoader sections,
            MemberDirectory members, FrontendProperties properties, Formats formats) {
        this.financeClient = financeClient;
        this.chitClient = chitClient;
        this.sections = sections;
        this.members = members;
        this.properties = properties;
        this.formats = formats;
    }

    /** One choice in the "payment for" list. */
    public record Target(String value, String label) {
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        AuthenticatedUser me = SecurityUtils.requireCurrentUser();
        int safePage = Paging.requested(page);
        PageResponse<TransactionResponse> payments = me.isAdmin()
                ? data(this.financeClient.payments(safePage, this.properties.pageSize()))
                : data(this.financeClient.myPayments(safePage, this.properties.pageSize()));
        String lastPage = Paging.pastTheEnd("/payments", page, payments);
        if (lastPage != null) {
            return lastPage;
        }
        model.addAttribute("nav", "payments");
        model.addAttribute("page", payments);
        return "payments/list";
    }

    @GetMapping("/new")
    public String newPayment(@RequestParam(name = "member", required = false) @Nullable Long member, Model model) {
        return renderForm(model, PaymentForm.forMember(member, this.formats.today()), Map.of(), null);
    }

    @PostMapping
    public String record(@ModelAttribute("form") @Nullable PaymentForm submitted, BindingResult binding,
            Model model, HttpServletResponse response) {
        PaymentForm form = (submitted != null) ? submitted : PaymentForm.forMember(null, this.formats.today());
        if (binding.hasErrors()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return renderForm(model, form, FormErrors.of(binding), null);
        }
        TransactionRequest request = form.toRequest();
        if (request == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return renderForm(model, form, Map.of("target", "Choose what this payment is for"), null);
        }
        try {
            data(this.financeClient.recordPayment(request));
            return "redirect:/payments?notice=" + Notice.PAYMENT_RECORDED.key();
        }
        catch (BackendException ex) {
            if (!ex.isRejection()) {
                throw ex;
            }
            Map<String, String> errors = new LinkedHashMap<>(ex.getFieldErrors());
            // The API reports the scheme or loan as targetId / transactionType; the form shows one "target" list
            String targetError = errors.remove("targetId");
            String typeError = errors.remove("transactionType");
            if (targetError != null || typeError != null) {
                errors.put("target", (targetError != null) ? targetError : typeError);
            }
            response.setStatus(ex.getStatus());
            return renderForm(model, form, errors, ex.userMessage());
        }
    }

    private String renderForm(Model model, PaymentForm form, Map<String, String> errors, @Nullable String error) {
        Pending<PageResponse<MemberResponse>> members = this.sections.start("members", this.members::all);
        Loaded<List<Target>> targets = Loaded.of(List.of());
        if (form.userId() != null) {
            long memberId = form.userId();
            Pending<List<ChitEnrollmentResponse>> slots = this.sections.start("the member's schemes",
                    () -> data(this.chitClient.enrollmentsOf(memberId)));
            Pending<List<LoanResponse>> loans = this.sections.start("the member's loans",
                    () -> data(this.financeClient.loansOf(memberId)));
            targets = targets(slots.join(), loans.join());
        }
        model.addAttribute("nav", "payments");
        model.addAttribute("form", form);
        model.addAttribute("members", members.join());
        model.addAttribute("targets", targets);
        model.addAttribute("paymentModes", PaymentMode.values());
        model.addAttribute("errors", errors);
        model.addAttribute("error", error);
        return NEW;
    }

    private Loaded<List<Target>> targets(Loaded<List<ChitEnrollmentResponse>> slots,
            Loaded<List<LoanResponse>> loans) {
        if (!slots.ok() || !loans.ok()) {
            return Loaded.failed((!slots.ok()) ? slots.error() : loans.error());
        }
        List<Target> targets = new ArrayList<>();
        Set<Long> plans = new HashSet<>();
        for (ChitEnrollmentResponse slot : slots.value()) {
            if (plans.add(slot.planId())) {
                targets.add(new Target(PaymentForm.target(TransactionType.CHIT_PAYMENT, slot.planId()),
                        "Chit: " + slot.planName()));
            }
        }
        for (LoanResponse loan : loans.value()) {
            if (loan.active()) {
                targets.add(new Target(PaymentForm.target(TransactionType.LOAN_PAYMENT, loan.id()),
                        "Loan " + loan.id() + ": " + this.formats.inr(loan.principalAmount()) + ", "
                                + this.formats.inr(loan.dailyInstallment()) + " a day"));
            }
        }
        return Loaded.of(List.copyOf(targets));
    }
}
