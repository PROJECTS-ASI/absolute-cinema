
package com.absolutecinema.profile;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.absolutecinema.auth.User;

import jakarta.validation.Valid;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {

        User user = profileService.getUserProfile(authentication.getName());

        if (!model.containsAttribute("profileUpdateRequest")) {
            ProfileUpdateRequest request = new ProfileUpdateRequest();

            request.setName(user.getName());
            request.setLastName(user.getLastName());
            request.setAge(user.getAge());
            request.setCellphone(user.getCellphone());
            request.setCity(user.getCity());

            model.addAttribute("profileUpdateRequest", request);
        }

        model.addAttribute("user", user);

        return "profile/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @Valid @ModelAttribute("profileUpdateRequest") ProfileUpdateRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            User user = profileService.getUserProfile(authentication.getName());

            model.addAttribute("user", user);

            return "profile/profile";
        }

        profileService.updateProfile(authentication.getName(), request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Tus datos se actualizaron correctamente."
        );

        return "redirect:/profile";
    }

    @GetMapping("/profile/history")
    public String showPurchaseHistory(
            Authentication authentication,
            Model model) {

        List<PurchaseHistoryItem> purchases =
                profileService.getPurchaseHistory(authentication.getName());

        model.addAttribute("purchases", purchases);

        return "profile/history";
    }
    

	@GetMapping("/profile/history/{bookingId}")
	public String showPurchaseDetail(
	        @org.springframework.web.bind.annotation.PathVariable Long bookingId,
	        Authentication authentication,
	        Model model) {
	
	    PurchaseDetailItem purchase =
	            profileService.getPurchaseDetail(
	                    authentication.getName(),
	                    bookingId
	            );
	
	    model.addAttribute("purchase", purchase);
	
	    return "profile/detail";
}

}
