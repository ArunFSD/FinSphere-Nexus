package com.finsphere.frontend.web;

import java.util.List;

import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.ChitEnrollmentResponse;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.common.dto.finance.LoanResponse;
import com.finsphere.common.dto.finance.TransactionResponse;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.frontend.client.ChitClient;
import com.finsphere.frontend.client.FinanceClient;
import com.finsphere.frontend.web.support.SectionLoader;
import com.finsphere.frontend.web.support.SectionLoader.Pending;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static com.finsphere.frontend.client.BackendResults.data;

/**
 * The first page after signing in. Administrators see running schemes, recent collections and loans; members see
 * their slots, loans and payments, and schemes they can still join. Sections load in parallel.
 */
@Controller
public class DashboardController {

    private final ChitClient chitClient;

    private final FinanceClient financeClient;

    private final SectionLoader sections;

    public DashboardController(ChitClient chitClient, FinanceClient financeClient, SectionLoader sections) {
        this.chitClient = chitClient;
        this.financeClient = financeClient;
        this.sections = sections;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AuthenticatedUser me = SecurityUtils.requireCurrentUser();
        model.addAttribute("nav", "dashboard");
        if (me.isAdmin()) {
            Pending<PageResponse<ChitPlanResponse>> schemes = this.sections.start("running schemes",
                    () -> data(this.chitClient.activePlans(0, 8)));
            Pending<PageResponse<TransactionResponse>> payments = this.sections.start("recent payments",
                    () -> data(this.financeClient.payments(0, 6)));
            Pending<PageResponse<LoanResponse>> loans = this.sections.start("recent loans",
                    () -> data(this.financeClient.loans(0, 6)));
            Pending<PageResponse<MemberResponse>> members = this.sections.start("members",
                    () -> data(this.chitClient.members(0, 1)));
            model.addAttribute("schemes", schemes.join());
            model.addAttribute("payments", payments.join());
            model.addAttribute("loans", loans.join());
            model.addAttribute("members", members.join());
            return "dashboard-admin";
        }
        Pending<List<ChitEnrollmentResponse>> slots = this.sections.start("your slots",
                () -> data(this.chitClient.myEnrollments()));
        Pending<List<LoanResponse>> loans = this.sections.start("your loans",
                () -> data(this.financeClient.myLoans()));
        Pending<PageResponse<TransactionResponse>> payments = this.sections.start("your payments",
                () -> data(this.financeClient.myPayments(0, 5)));
        Pending<PageResponse<ChitPlanResponse>> open = this.sections.start("open schemes",
                () -> data(this.chitClient.activePlans(0, 6)));
        model.addAttribute("slots", slots.join());
        model.addAttribute("loans", loans.join());
        model.addAttribute("payments", payments.join());
        model.addAttribute("schemes", open.join());
        return "dashboard-member";
    }
}
