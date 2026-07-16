package br.com.estudalivre;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

@Controller
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SpaController {

    @GetMapping("/redefinir-senha")
    public String passwordReset() {
        return "forward:/index.html";
    }
}
