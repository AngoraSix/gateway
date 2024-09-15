package com.angorasix.gateway.infrastructure.filters;

import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure;
import com.angorasix.commons.presentation.dto.IsAdminDto;
import com.angorasix.gateway.infrastructure.config.api.GatewayApiConfigurations;
import com.angorasix.gateway.infrastructure.config.constants.ConfigConstants;
import com.angorasix.gateway.infrastructure.config.internalroutes.GatewayInternalRoutesConfigurations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>Filter to validate the user is Admin of the manipulated Resource, for GET requests (avoiding
 * messing up the request body, which can generate issues depending on the underlying
 * infrastructure).
 * </p>
 *
 * @author rozagerardo
 */
@Component
public class ValidateIsAdminGetGatewayFilterFactory extends
        AbstractGatewayFilterFactory<ValidateIsAdminGetGatewayFilterFactory.Config> {

    /* default */ final Logger logger = LoggerFactory.getLogger(
            ValidateIsAdminGetGatewayFilterFactory.class);

//    private final transient ParameterizedTypeReference<Map<String, Object>> jsonType =
//            new ParameterizedTypeReference<>() {
//            };

    private final transient GatewayInternalRoutesConfigurations internalRoutesConfigs;

    private final transient GatewayApiConfigurations apiConfigs;

    private final transient ConfigConstants configConstants;

    /**
     * Main constructor with required params.
     *
     * @param apiConfigs            API configs
     * @param internalRoutesConfigs internal Routes configs to route composing call
     */
    public ValidateIsAdminGetGatewayFilterFactory(
            final GatewayApiConfigurations apiConfigs,
            final ConfigConstants configConstants,
            final GatewayInternalRoutesConfigurations internalRoutesConfigs) {
        super(Config.class);
        this.apiConfigs = apiConfigs;
        this.configConstants = configConstants;
        this.internalRoutesConfigs = internalRoutesConfigs;
    }

    @Override
    public GatewayFilter apply(final Config config) {
        return (filterExchange, chain) -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(JwtAuthenticationToken.class::cast)
                .flatMap(
                        auth -> {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Validating if is Admin...");
                                logger.debug(config.toString());
                            }
                            final String associatedEntityId = obtainAssociatedEntityId(config, filterExchange);
                            final String serviceBaseUrl = obtainServiceBaseUrl(config, apiConfigs);
                            final String serviceOutBaseUrl = obtainServiceOutBaseUrl(config, apiConfigs);
                            final String resolvedAdminEndpoint = obtainAdminEndpoint(config, associatedEntityId);
                            // Request to a path managed by the Gateway
                            final WebClient client = WebClient.create();
                            return client.get().uri(
                                            UriComponentsBuilder.fromUriString(serviceBaseUrl)
                                                    .pathSegment(serviceOutBaseUrl, resolvedAdminEndpoint)
                                                    .build().toUri())
                                    .header(HttpHeaders.AUTHORIZATION,
                                            "Bearer %s".formatted(auth.getToken().getTokenValue()))
                                    .header(config.getGoogleCloudRunAuthHeader(),
                                            "Bearer %s".formatted(
                                                    Optional.ofNullable(filterExchange.getAttribute("%s-%s".formatted(
                                                                    configConstants.googleTokenAttribute(),
                                                                    apiConfigs.projects().core().baseUrl())))
                                                            .map(Object::toString)
                                                            .orElse("")))
                                    .exchangeToMono(response -> response.bodyToMono(IsAdminDto.class))
                                    .switchIfEmpty(Mono.just(new IsAdminDto(false)))
                                    .map(isAdminResponse -> {;
                                        if (!config.isNonAdminRequestAllowed() && !isAdminResponse.isAdmin()) {
                                            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                                    "Only admin can proceed");
                                        }
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("isAdmin result: %s".formatted(isAdminResponse.isAdmin()));
                                        }
                                        filterExchange.getAttributes()
                                                .put(configConstants.isAssociatedEntityAdminAttribute(),
                                                        isAdminResponse.isAdmin());
                                        return filterExchange;
                                    });
                        }).flatMap(chain::filter);
    }

    private String obtainAssociatedEntityId(final Config config, final ServerWebExchange exchange) {
        return Optional.ofNullable(
                        exchange.getAttribute(ServerWebExchangeUtils.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .map(Map.class::cast).map(attributes -> attributes.get(config.isForProjectManagement() ? configConstants.projectManagementIdParam() : configConstants.projectIdParam()))
                .map(String.class::cast)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Can't obtain %s from request URI".formatted(config.isForProjectManagement() ? configConstants.projectManagementIdParam() : configConstants.projectIdParam())));
    }

    private String obtainServiceBaseUrl(final Config config, final GatewayApiConfigurations apiConfigs) {
        return config.isForProjectManagement() ? apiConfigs.managements().core().baseUrl()
                : apiConfigs.projects().core().baseUrl();
    }

    private String obtainServiceOutBaseUrl(final Config config, final GatewayApiConfigurations apiConfigs) {
        return config.isForProjectManagement() ? apiConfigs.managements().core().outBasePath()
                : apiConfigs.projects().core().outBasePath();
    }

    private String obtainAdminEndpoint(final Config config, final String associatedEntityId) {
        return config.isForProjectManagement() ? internalRoutesConfigs.managementsCore()
                .isAdminEndpoint()
                .replace(configConstants.projectManagementIdParam(), associatedEntityId)
                : internalRoutesConfigs.projectsCore()
                .isAdminEndpoint()
                .replace(configConstants.projectIdPlaceholder(), associatedEntityId);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("nonAdminRequestAllowed", "forProjectManagement", "anonymousRequestAllowed",
                "googleCloudRunAuthHeader");
    }

    /**
     * <p>
     * Config class to use for AbstractGatewayFilterFactory.
     * </p>
     */
    public static class Config {

        private boolean nonAdminRequestAllowed;
        private boolean forProjectManagement;
        private boolean anonymousRequestAllowed;

        private String googleCloudRunAuthHeader =
                AngoraSixInfrastructure.GOOGLE_CLOUD_RUN_INFRA_AUTH_HEADER;

        public boolean isNonAdminRequestAllowed() {
            return nonAdminRequestAllowed;
        }

        public void setNonAdminRequestAllowed(final boolean nonAdminRequestAllowed) {
            this.nonAdminRequestAllowed = nonAdminRequestAllowed;
        }

        public boolean isAnonymousRequestAllowed() {
            return anonymousRequestAllowed;
        }

        public void setAnonymousRequestAllowed(final boolean anonymousRequestAllowed) {
            this.anonymousRequestAllowed = anonymousRequestAllowed;
        }

        public String getGoogleCloudRunAuthHeader() {
            return googleCloudRunAuthHeader;
        }

        public void setGoogleCloudRunAuthHeader(final String googleCloudRunAuthHeader) {
            this.googleCloudRunAuthHeader = googleCloudRunAuthHeader;
        }

        public boolean isForProjectManagement() {
            return forProjectManagement;
        }

        public void setForProjectManagement(boolean forProjectManagement) {
            this.forProjectManagement = forProjectManagement;
        }
    }
}