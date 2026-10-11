package com.finsphere.frontend.web;

import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;

import com.finsphere.common.api.PageResponse;
import com.finsphere.common.dto.chit.MemberResponse;
import com.finsphere.frontend.client.AuthClient;
import com.finsphere.frontend.client.BackendException;
import com.finsphere.frontend.client.ChitClient;
import com.finsphere.frontend.config.FrontendProperties;
import com.finsphere.frontend.web.form.RegistrationForm;
import com.finsphere.frontend.web.support.Notice;
import com.finsphere.frontend.web.support.Paging;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.finsphere.frontend.client.BackendResults.data;

/** Member directory and adding members (administrators only, enforced by the security configuration). */
@Controller
@RequestMapping("/members")
public class MemberController {

    private static final String NEW = "members/new";

    private static final Set<String> ROLES = Set.of("CUSTOMER", "ADMIN");

    private final ChitClient chitClient;

    private final AuthClient authClient;

    private final FrontendProperties properties;

    public MemberController(ChitClient chitClient, AuthClient authClient, FrontendProperties properties) {
        this.chitClient = chitClient;
        this.authClient = authClient;
        this.properties = properties;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        PageResponse<MemberResponse> members = data(
                this.chitClient.members(Paging.requested(page), this.properties.pageSize()));
        String lastPage = Paging.pastTheEnd("/members", page, members);
        if (lastPage != null) {
            return lastPage;
        }
        model.addAttribute("nav", "members");
        model.addAttribute("page", members);
        return "members/list";
    }

    @GetMapping("/new")
    public String newMember(Model model) {
        model.addAttribute("nav", "members");
        model.addAttribute("form", RegistrationForm.empty("CUSTOMER"));
        model.addAttribute("errors", Map.of());
        return NEW;
    }

    @PostMapping
    public String add(@ModelAttribute("form") @Nullable RegistrationForm submitted, Model model,
            HttpServletResponse response) {
        RegistrationForm form = (submitted != null) ? submitted : RegistrationForm.empty("CUSTOMER");
        model.addAttribute("nav", "members");
        model.addAttribute("form", form.withoutPasswords());
        if (!form.passwordsMatch()) {
            model.addAttribute("errors", Map.of("confirmPassword", "The two passwords do not match"));
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return NEW;
        }
        String role = ROLES.contains(form.role()) ? form.role() : "CUSTOMER";
        try {
            this.authClient.register(form.toRequest(role));
            return "redirect:/members?notice=" + Notice.MEMBER_ADDED.key();
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
}
