package mt4.license.com.web;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import mt4.license.com.entity.License;
import mt4.license.com.service.RedisService;

@Controller
public class MainController {

    @Autowired
    private RedisService redisService;

    @GetMapping("/")
    public String root() {
        return "redirect:/add";
    }

    @GetMapping("/add")
    public String addLicense(Model model) {
        model.addAttribute("license", new License());
        return "key/add";
    }

    @PostMapping("/add")
    public String submitLicense(@ModelAttribute License license, Model model) {
        if (!redisService.add(license)) {
            model.addAttribute("error", "");
            model.addAttribute("message", "Failure in adding key");
        } else {
            model.addAttribute("message", "Success in adding key");
        }
        model.addAttribute("pageTitle", "Adding key");
        return "key/result";
    }

    @RequestMapping(value = "/toggle", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> toggleState(@RequestBody License license) {
        license.setEnable(!license.isEnable());
        if (!redisService.update(license)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(license);
    }

    @GetMapping("/search")
    public String search(Model model) {
        model.addAttribute("licenses", redisService.listAll());
        return "key/search";
    }

    @PostMapping("/search")
    public String search(Model model, @RequestParam("query") String key) {
        Set<License> set = key.startsWith("@") ? redisService.searchByCompany(key.substring(1))
                : redisService.searchByKey(key);
        model.addAttribute("licenses", set);
        return "key/search::table_refresh";
    }

    @PostMapping("/edit")
    public String editLicense(Model model, @RequestParam("query") String key) {
        License license = new License(key);
        if (redisService.get(license)) {
            model.addAttribute("license", license);
            return "key/edit";
        } else {
            return "redirect:/add";
        }
    }

    @PostMapping("/edit_result")
    public String editResult(@ModelAttribute License license, Model model) {
        if (!redisService.update(license)) {
            model.addAttribute("error", "");
            model.addAttribute("message", "Failure in updating key");
        } else {
            model.addAttribute("message", "Success in updating key");
        }
        model.addAttribute("pageTitle", "Updating key");
        return "key/result";
    }

    @GetMapping("/login")
    public String login() {
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !SecurityContextHolder.getContext().getAuthentication().getName().equals("anonymousUser")) {
            return "redirect:/add";
        }

        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "/error/access-denied";
    }

}
