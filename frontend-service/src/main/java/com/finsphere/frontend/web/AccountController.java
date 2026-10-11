package com.finsphere.frontend.web;

import com.finsphere.frontend.client.AuthClient;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import static com.finsphere.frontend.client.BackendResults.data;

/** The signed-in person's own details. */
@Controller
public class AccountController {

    private final AuthClient authClient;

    public AccountController(AuthClient authClient) {
        this.authClient = authClient;
    }

    @GetMapping("/account")
    public String account(Model model) {
        model.addAttribute("nav", "account");
        model.addAttribute("profile", data(this.authClient.me()));
        return "account/profile";
    }
}
