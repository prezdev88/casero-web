package cl.casero.migration.api.controller;

import cl.casero.migration.api.dto.UpdatePinRequest;
import cl.casero.migration.api.dto.UserResponse;
import cl.casero.migration.domain.AppUser;
import cl.casero.migration.service.AppUserService;
import cl.casero.migration.service.dto.CreateUserForm;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserApiController {

    private final AppUserService appUserService;

    public AdminUserApiController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return appUserService.listAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserForm form) {
        AppUser user = appUserService.create(form.getName(), form.getRole(), form.getPin());
        return toResponse(user);
    }

    @PutMapping("/{id}/pin")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePin(@PathVariable Long id,
                          @Valid @RequestBody UpdatePinRequest request) {
        appUserService.updatePin(id, request.pin());
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
