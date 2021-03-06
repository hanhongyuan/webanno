/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp;

import java.io.File;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.resource.DynamicJQueryResourceReference;
import org.apache.wicket.settings.IExceptionSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.jboss.logging.MDC;
import org.springframework.context.ApplicationContext;
import org.wicketstuff.annotation.scan.AnnotatedMountScanner;

import de.tudarmstadt.ukp.clarin.webanno.api.Logging;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssUiReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssVisReference;
import de.tudarmstadt.ukp.clarin.webanno.model.support.spring.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.support.FileSystemResource;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.security.SpringAuthenticatedWebSession;

/**
 * The wicket application class. Sets up pages, authentication, theme, and other application-wide
 * configuration.
 *
 */
public class WicketApplication
    extends AuthenticatedWebApplication
{
    boolean isInitialized = false;

    @Override
    protected void init()
    {
        super.init();
        getComponentInstantiationListeners().add(new SpringComponentInjector(this));
        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getComponentPostOnBeforeRenderListeners().add(new StatelessChecker());
        }
        
        if (!isInitialized) {
            // Enable dynamic switching between JQuery 1 and JQuery 2 based on the browser
            // identification. 
            getJavaScriptLibrarySettings().setJQueryReference(
                    new DynamicJQueryResourceReference());

            mountPage("/login.html", getSignInPageClass());
            mountPage("/welcome.html", getHomePage());

            // Mount the other pages via @MountPath annotation on the page classes
            new AnnotatedMountScanner().scanPackage("de.tudarmstadt.ukp.clarin.webanno").mount(this);

            // FIXME Handling brat font/css resources should be moved to brat module
            mountResource("/style-vis.css", BratCssVisReference.get());
            mountResource("/style-ui.css", BratCssUiReference.get());

            Properties settings = SettingsUtil.getSettings();
            String logoValue = settings.getProperty(SettingsUtil.CFG_STYLE_LOGO);
            if (StringUtils.isNotBlank(logoValue) && new File(logoValue).canRead()) {
                getSharedResources().add("logo", new FileSystemResource(new File(logoValue)));
                mountResource("/images/logo.png", new SharedResourceReference("logo"));
            }
            else {
                mountResource("/images/logo.png", new ContextRelativeResourceReference(
                        "images/logo.png", false));
            }
            
            // Display stack trace instead of internal error
            if ("true".equalsIgnoreCase(settings.getProperty("debug.showExceptionPage"))) {
                getExceptionSettings().setUnexpectedExceptionDisplay(
                        IExceptionSettings.SHOW_EXCEPTION_PAGE);
            }

            getRequestCycleListeners().add(new AbstractRequestCycleListener()
            {
                @Override
                public void onBeginRequest(RequestCycle cycle)
                {
                    ApplicationContext ctx = ApplicationContextProvider.getApplicationContext();
                    RepositoryService repo = ctx.getBean(RepositoryService.class);
                    MDC.put(Logging.KEY_REPOSITORY_PATH, repo.getDir().getAbsolutePath());
                };

                @Override
                public void onEndRequest(RequestCycle cycle)
                {
                    MDC.remove(Logging.KEY_REPOSITORY_PATH);
                };
            });
            
            isInitialized = true;
        }
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class<? extends Page> getHomePage()
    {
        return WelcomePage.class;
    }

    @Override
    public Class<? extends WebPage> getSignInPageClass()
    {
        return LoginPage.class;
    }

    @Override
    protected Class<? extends AuthenticatedWebSession> getWebSessionClass()
    {
        return SpringAuthenticatedWebSession.class;
    }
}
