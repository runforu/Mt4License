package mt4.license.com.web;

import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import mt4.license.com.entity.AccessInfo;
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
    public String submitLicense(@Valid @ModelAttribute("license") License license, BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("license", license);
            return "key/add";
        }
        if (!redisService.addLicense(license)) {
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
    public ResponseEntity<?> toggleState(@RequestParam("key") String key) {
        License license = new License(key);
        if (!redisService.getLicense(license)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        license.setEnable(!license.isEnable());
        redisService.deleteAccessInfo(license);
        if (!redisService.updateLicense(license)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(license);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public String delete(Model model, @RequestParam("query") String query, @RequestParam("key") String key) {
        License license = new License(key);
        if (redisService.getLicense(license)) {
            redisService.deleteLicense(license);
            redisService.deleteAccessInfo(license);
        }
        Set<License> set = query.startsWith("@") ? redisService.searchLicenseByCompany(query.substring(1))
                : redisService.searchLicenseByKey(query);
        model.addAttribute("licenses", set);
        return "key/search::table_refresh";
    }

    @GetMapping("/search")
    public String search(Model model) {
        model.addAttribute("licenses", redisService.listLicense());
        model.addAttribute("pageTitle", "Search");
        return "key/search";
    }

    @PostMapping("/search")
    public String search(Model model, @RequestParam("query") String key) {
        Set<License> set = key.startsWith("@") ? redisService.searchLicenseByCompany(key.substring(1))
                : redisService.searchLicenseByKey(key);
        model.addAttribute("licenses", set);
        return "key/search::table_refresh";
    }

    @PostMapping("/edit")
    public String editLicense(Model model, @RequestParam("query") String key) {
        License license = new License(key);
        if (redisService.getLicense(license)) {
            model.addAttribute("license", license);
            return "key/edit";
        } else {
            return "redirect:/add";
        }
    }

    @PostMapping("/edit_result")
    public String editResult(@ModelAttribute License license, Model model) {
        if (!redisService.updateLicense(license)) {
            model.addAttribute("error", "");
            model.addAttribute("message", "Failure in updating key");
        } else {
            model.addAttribute("message", "Success in updating key");
        }
        model.addAttribute("pageTitle", "Updating key");
        return "key/result";
    }

    @PostMapping("/history")
    public String history(Model model, @RequestParam("query") String key) {
        License license = new License(key);
        List<AccessInfo> list = redisService.range(license, 0, 20);
        if (list.size() == 0) {
            model.addAttribute("message", "Access history is empty.");
        } else {
            model.addAttribute("message", "Access history");
        }
        model.addAttribute("key", license.getKey());
        model.addAttribute("pageTitle", "Access history");
        model.addAttribute("accesses", list);
        return "key/history";
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
