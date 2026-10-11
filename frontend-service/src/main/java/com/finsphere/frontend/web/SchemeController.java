package com.finsphere.frontend.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.AuctionRequest;
import com.finsphere.common.dto.chit.ChitPlanDetailResponse;
import com.finsphere.common.dto.chit.ChitPlanRequest;
import com.finsphere.common.dto.chit.ChitPlanResponse;
import com.finsphere.common.dto.chit.EnrollmentRequest;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.common.security.AuthenticatedUser;
import com.finsphere.common.security.SecurityUtils;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.ChitClient;
import com.finsphere.frontend.config.FrontendProperties;
import com.finsphere.frontend.web.form.AuctionForm;
import com.finsphere.frontend.web.form.SlotForm;
import com.finsphere.frontend.web.support.Formats;
import com.finsphere.frontend.web.support.Loaded;
import com.finsphere.frontend.web.support.MemberDirectory;
import com.finsphere.frontend.web.support.Notice;
import com.finsphere.frontend.web.support.Paging;
import com.finsphere.frontend.web.support.SectionLoader;
import com.finsphere.frontend.web.support.SectionLoader.Pending;
import com.finsphere.frontend.web.view.SchemeView;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.finsphere.frontend.client.BackendResults.data;

/** Chit schemes: the list, a scheme's slot board and ledger, booking slots, settling auctions, new schemes. */
@Controller
@RequestMapping("/schemes")
public class SchemeController {

    private static final String DETAIL = "schemes/detail";

    private static final String NEW = "schemes/new";

    private final ChitClient chitClient;

    private final SectionLoader sections;

    private final MemberDirectory members;

    private final FrontendProperties properties;

    private final Formats formats;

    public SchemeController(ChitClient chitClient, SectionLoader sections, MemberDirectory members,
            FrontendProperties properties, Formats formats) {
        this.chitClient = chitClient;
        this.sections = sections;
        this.members = members;
        this.properties = properties;
        this.formats = formats;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        PageResponse<ChitPlanResponse> plans = data(
                this.chitClient.activePlans(Paging.requested(page), this.properties.pageSize()));
        String lastPage = Paging.pastTheEnd("/schemes", page, plans);
        if (lastPage != null) {
            return lastPage;
        }
        model.addAttribute("nav", "schemes");
        model.addAttribute("plans", plans);
        return "schemes/list";
    }

    @GetMapping("/{planId}")
    public String detail(@PathVariable("planId") long planId, Model model) {
        return renderDetail(planId, model, SlotForm.EMPTY, null, Map.of(), null);
    }

    /**
     * The booking and auction forms post to their own addresses, so after a refused form the address bar shows
     * them. Opening such an address again (reload in a new tab, a copied link) goes to the scheme's section.
     */
    @GetMapping("/{planId}/slots")
    public String slotsSection(@PathVariable("planId") long planId) {
        return "redirect:/schemes/" + planId + "#slots";
    }

    @GetMapping("/{planId}/auction")
    public String auctionSection(@PathVariable("planId") long planId) {
        return "redirect:/schemes/" + planId + "#auction";
    }

    @PostMapping("/{planId}/slots")
    public String bookSlot(@PathVariable("planId") long planId,
            @ModelAttribute("slotForm") @Nullable SlotForm submitted, BindingResult binding, Model model,
            HttpServletResponse response) {
        // A value that is not a number leaves no form object at all (record binding), so fall back to an empty one
        SlotForm form = (submitted != null) ? submitted : SlotForm.EMPTY;
        AuthenticatedUser me = SecurityUtils.requireCurrentUser();
        Long memberId = me.isAdmin() ? form.userId() : me.userId();
        if (binding.hasErrors() || form.slotNumber() == null || memberId == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return renderDetail(planId, model, form, null,
                    Map.of((memberId == null) ? "userId" : "slotNumber",
                            (memberId == null) ? "Choose a member" : "Choose a free slot"),
                    null);
        }
        try {
            data(this.chitClient.enroll(new EnrollmentRequest(planId, memberId, form.slotNumber())));
            return "redirect:/schemes/" + planId + "?notice=" + Notice.SLOT_BOOKED.key() + "#slots";
        }
        catch (BackendException ex) {
            if (!ex.isRejection()) {
                throw ex;
            }
            response.setStatus(ex.getStatus());
            return renderDetail(planId, model, form, ex.userMessage(), ex.getFieldErrors(), null);
        }
    }

    @PostMapping("/{planId}/auction")
    public String settleAuction(@PathVariable("planId") long planId,
            @ModelAttribute("auctionForm") @Nullable AuctionForm submitted, BindingResult binding, Model model,
            HttpServletResponse response) {
        AuctionForm form = (submitted != null) ? submitted : new AuctionForm(null, null, null);
        if (binding.hasErrors() || form.monthlyCount() == null || form.winnerUserId() == null
                || form.bidAmount() == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return renderDetail(planId, model, SlotForm.EMPTY, null, Map.of(), new AuctionOutcome(form,
                    "Choose the month and the winner, and enter the winning bid.", Map.of()));
        }
        try {
            data(this.chitClient.settleAuction(
                    new AuctionRequest(planId, form.monthlyCount(), form.bidAmount(), form.winnerUserId())));
            return "redirect:/schemes/" + planId + "?notice=" + Notice.AUCTION_SETTLED.key() + "#ledger";
        }
        catch (BackendException ex) {
            if (!ex.isRejection()) {
                throw ex;
            }
            response.setStatus(ex.getStatus());
            return renderDetail(planId, model, SlotForm.EMPTY, null, Map.of(),
                    new AuctionOutcome(form, ex.userMessage(), ex.getFieldErrors()));
        }
    }

    @GetMapping("/new")
    public String newScheme(Model model) {
        LocalDate firstOfNextMonth = this.formats.today().withDayOfMonth(1).plusMonths(1);
        ChitPlanRequest suggestion = new ChitPlanRequest("", null, 20, new BigDecimal("5"), null, null, 20,
                firstOfNextMonth, 10);
        model.addAttribute("nav", "schemes");
        model.addAttribute("form", suggestion);
        model.addAttribute("errors", Map.of());
        return NEW;
    }

    @PostMapping
    public String create(@ModelAttribute("form") @Nullable ChitPlanRequest submitted, BindingResult binding,
            Model model, HttpServletResponse response) {
        ChitPlanRequest form = (submitted != null) ? submitted
                : new ChitPlanRequest(null, null, null, null, null, null, null, null, null);
        model.addAttribute("nav", "schemes");
        model.addAttribute("form", form);
        if (binding.hasErrors()) {
            model.addAttribute("errors", FormErrors.of(binding));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return NEW;
        }
        try {
            ChitPlanResponse created = data(this.chitClient.createPlan(form));
            return "redirect:/schemes/" + created.id() + "?notice=" + Notice.SCHEME_CREATED.key();
        }
        catch (BackendException ex) {
            if (!ex.isRejection()) {
                throw ex;
            }
            model.addAttribute("errors", ex.getFieldErrors());
            model.addAttribute("error", ex.userMessage());
            response.setStatus(ex.getStatus());
            return NEW;
        }
    }

    /** What went wrong with an auction form, to show it again with the message. */
    public record AuctionOutcome(AuctionForm form, String error, Map<String, String> errors) {
    }

    private String renderDetail(long planId, Model model, SlotForm slotForm, @Nullable String slotError,
            Map<String, String> slotErrors, @Nullable AuctionOutcome auction) {
        AuthenticatedUser me = SecurityUtils.requireCurrentUser();
        Pending<PageResponse<MemberResponse>> members = me.isAdmin()
                ? this.sections.start("members", this.members::all) : null;
        ChitPlanDetailResponse detail = data(this.chitClient.plan(planId));
        SchemeView scheme = SchemeView.of(detail, me);
        Loaded<PageResponse<MemberResponse>> memberOptions = (members != null) ? members.join()
                : Loaded.of(PageResponse.empty(0));

        AuctionForm auctionForm = (auction != null) ? auction.form()
                : new AuctionForm((scheme.openMonth() != null) ? scheme.openMonth().number() : null, null, null);
        model.addAttribute("nav", "schemes");
        model.addAttribute("scheme", scheme);
        model.addAttribute("memberOptions", memberOptions);
        model.addAttribute("slotForm", slotForm);
        model.addAttribute("slotError", slotError);
        model.addAttribute("slotErrors", slotErrors);
        model.addAttribute("auctionForm", auctionForm);
        model.addAttribute("auctionError", (auction != null) ? auction.error() : null);
        model.addAttribute("auctionErrors", (auction != null) ? auction.errors() : Map.of());
        return DETAIL;
    }
}
