package org.jenkinsci.plugins.github_branch_source;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import jenkins.scm.api.SCMHeadOrigin;
import jenkins.scm.api.mixin.ChangeRequestCheckoutStrategy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.FileNotFoundException;

import static org.junit.Assert.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class Cached404ForNonMergeablePRTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    public static WireMockRuleFactory factory = new WireMockRuleFactory();
    @Rule
    public WireMockRule githubApi = factory.getRule(WireMockConfiguration.options().dynamicPort().usingFilesUnderClasspath("cacheTests"));
    private GitHubSCMProbe probe;
    private GHRepository repo;
    private GitHub github;

    @Before
    public void setUp() throws Exception {
        GitHubSCMProbe.JENKINS_54126_WORKAROUND = true;
        GitHubSCMProbe.STAT_RETHROW_API_FNF = true;
        github = Connector.connect("http://localhost:" + githubApi.port(), null);
        repo = github.getRepository("cloudbeers/yolo");

        // Inserting from GitHubSCMSourceTest
        githubApi.stubFor(
                get(urlEqualTo("/repos/cloudbeers/yolo/pulls/3"))
                        .inScenario("Pull Request Merge Hash")
                        .whenScenarioStateIs(Scenario.STARTED)
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json; charset=utf-8")
                                        .withBodyFile("body-yolo-pulls-3.json"))
                        .willSetStateTo("Pull Request Merge Hash - retry 1")
                        .atPriority(0));

        githubApi.stubFor(
                get(urlEqualTo("/repos/cloudbeers/yolo/pulls/2"))
                        .inScenario("Pull Request Merge Hash")
                        .whenScenarioStateIs("Pull Request Merge Hash - retry 1")
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json; charset=utf-8")
                                        .withBodyFile("body-yolo-pulls-2-mergeable-null.json"))
                        .willSetStateTo("Pull Request Merge Hash - retry 2"));

        githubApi.stubFor(
                get(urlEqualTo("/repos/cloudbeers/yolo/pulls/2"))
                        .inScenario("Pull Request Merge Hash")
                        .whenScenarioStateIs("Pull Request Merge Hash - retry 2")
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json; charset=utf-8")
                                        .withBodyFile("body-yolo-pulls-2-mergeable-true.json"))
                        .willSetStateTo("Pull Request Merge Hash - retry 2"));
    }

    @Issue("JENKINS-60353")
    // Nothing I do here matters WTF
    @Test
    @Ignore
    public void NonMergeable() throws Exception {
        GitHub github = Connector.connect("http://localhost:" + githubApi.port(), null);
        PullRequestSCMHead head = new PullRequestSCMHead("PR-3", "cloudbeers", "yolo", "b", 3, new BranchSCMHead("master"), new SCMHeadOrigin.Fork("kshultzCB"), ChangeRequestCheckoutStrategy.MERGE);
        githubApi.stubFor(get(urlPathEqualTo("/repos/cloudbeers/yolo/contents?ref=refs/pull/3/merge")).willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("body-cloudbeers-yolo-non-mergeable-pr.json")) // body-cloudbeers-yolo-non-mergeable-pr.json --OR-- body-cloudbeers-yolo-non-mergeable-pr-contents.json
        );
        probe = new GitHubSCMProbe(github, repo, head, new PullRequestSCMRevision(head, "a", "b"));
        assertTrue(probe.stat("README.md").exists());
    }
}
