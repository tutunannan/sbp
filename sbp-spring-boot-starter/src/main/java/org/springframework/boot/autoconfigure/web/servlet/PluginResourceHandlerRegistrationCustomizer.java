/*
 * Copyright (C) 2019-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.web.servlet;

import org.laxture.sbp.internal.PluginResourceResolver;
import org.laxture.sbp.spring.boot.SbpPluginStateChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.ApplicationListener;
import org.springframework.web.servlet.config.annotation.ResourceChainRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.resource.AppCacheManifestTransformer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.VersionResourceResolver;

/**
 * @author <a href="https://github.com/hank-cp">Hank CP</a>
 */
public class PluginResourceHandlerRegistrationCustomizer implements
        WebMvcAutoConfiguration.ResourceHandlerRegistrationCustomizer,
        ApplicationListener<SbpPluginStateChangedEvent> {

    private static final String DEFAULT_CACHE_NAME = "sbp-resource-chain-cache";

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private ResourceProperties resourceProperties = new ResourceProperties();

    @Autowired(required = false)
    @Qualifier("sbpResourceCache")
    private Cache sbpResourceCache;

    @Override
    public void customize(ResourceHandlerRegistration registration) {
        if (sbpResourceCache == null) {
            sbpResourceCache = new ConcurrentMapCache(DEFAULT_CACHE_NAME);
        }
        ResourceProperties.Chain properties = this.resourceProperties.getChain();
        ResourceChainRegistration chain = registration.resourceChain(properties.isCache(), sbpResourceCache);

        chain.addResolver(new PluginResourceResolver());

        ResourceProperties.Strategy strategy = properties.getStrategy();
        if (properties.isCompressed()) {
            chain.addResolver(new EncodedResourceResolver());
        }
        if (strategy.getFixed().isEnabled() || strategy.getContent().isEnabled()) {
            chain.addResolver(getVersionResourceResolver(strategy));
        }
        if (properties.isHtmlApplicationCache()) {
            chain.addTransformer(new AppCacheManifestTransformer());
        }
    }

    private ResourceResolver getVersionResourceResolver(ResourceProperties.Strategy properties) {
        VersionResourceResolver resolver = new VersionResourceResolver();
        if (properties.getFixed().isEnabled()) {
            String version = properties.getFixed().getVersion();
            String[] paths = properties.getFixed().getPaths();
            resolver.addFixedVersionStrategy(version, paths);
        }
        if (properties.getContent().isEnabled()) {
            String[] paths = properties.getContent().getPaths();
            resolver.addContentVersionStrategy(paths);
        }
        return resolver;
    }

    @Override
    public void onApplicationEvent(SbpPluginStateChangedEvent event) {
        if (sbpResourceCache == null) return;
        sbpResourceCache.clear();
    }
}
