package com.gourav.restapi.controllers;

import com.gourav.restapi.config.jwt.JwtUtils;
import com.gourav.restapi.config.services.UserDetailsImpl;
import com.gourav.restapi.controllers.payload.request.LoginRequest;
import com.gourav.restapi.controllers.payload.request.SignupRequest;
import com.gourav.restapi.controllers.payload.response.JwtResponse;
import com.gourav.restapi.controllers.payload.response.MessageResponse;
import com.gourav.restapi.exceptions.ApiStatusException;
import com.gourav.restapi.exceptions.RoleNotFoundException;
import com.gourav.restapi.models.ERole;
import com.gourav.restapi.models.Role;
import com.gourav.restapi.models.User;
import com.gourav.restapi.repositories.RoleRepository;
import com.gourav.restapi.repositories.UserRepository;
import com.gourav.restapi.security.Authentication;
import com.gourav.restapi.security.AuthenticationManager;
import com.gourav.restapi.security.GrantedAuthority;
import com.gourav.restapi.security.PasswordEncoder;
import com.gourav.restapi.security.SecurityContext;
import com.gourav.restapi.security.SecurityContextHolder;
import com.gourav.restapi.security.UsernamePasswordAuthenticationToken;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    // Constructor injection instead of field injection
    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder encoder,
                          JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticateUser(@Valid LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Response.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles)).build();
    }

    @POST
    @Path("/signup")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(@Valid SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse("Error: Username is already taken!"))
                    .build();
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(new MessageResponse("Error: Email is already in use!"))
                    .build();
        }

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RoleNotFoundException(ERole.ROLE_USER.name()));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RoleNotFoundException(ERole.ROLE_ADMIN.name()));
                        roles.add(adminRole);
                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                                .orElseThrow(() -> new RoleNotFoundException(ERole.ROLE_MODERATOR.name()));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RoleNotFoundException(ERole.ROLE_USER.name()));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return Response.ok(new MessageResponse("User registered successfully!")).build();
    }

    @POST
    @Path("/logout")
    public Response logoutUser(@Context HttpHeaders headers) {
        String headerAuth = headers.getHeaderString("Authorization");

        // Validate bearer token format
        if (headerAuth == null || !headerAuth.startsWith("Bearer ")) {
            throw new ApiStatusException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Missing or invalid Authorization header. Expected: Authorization: Bearer <token>");
        }

        String token = headerAuth.substring(7);

        // Validate token format before attempting to parse
        try {
            // This validates the token signature and expiration
            jwtUtils.getUserNameFromJwtToken(token);
            jwtUtils.invalidateToken(token);
        } catch (JwtException e) {
            throw new ApiStatusException(
                    Response.Status.UNAUTHORIZED.getStatusCode(),
                    "Invalid or expired token: " + e.getMessage(),
                    e);
        } catch (IllegalArgumentException e) {
            throw new ApiStatusException(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "Malformed token: " + e.getMessage(),
                    e);
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(null);

        return Response.ok(new MessageResponse("logout successful")).build();
    }
}
