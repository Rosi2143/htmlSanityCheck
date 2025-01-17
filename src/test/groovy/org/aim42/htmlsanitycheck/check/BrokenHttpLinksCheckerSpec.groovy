package org.aim42.htmlsanitycheck.check

import org.aim42.htmlsanitycheck.Configuration
import org.aim42.htmlsanitycheck.collect.SingleCheckResults
import org.aim42.htmlsanitycheck.html.HtmlConst
import org.aim42.htmlsanitycheck.html.HtmlPage
import org.aim42.inet.NetUtil
import spock.lang.IgnoreIf
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll


// see end-of-file for license information


class BrokenHttpLinksCheckerSpec extends Specification {

    Checker brokenHttpLinksChecker
    HtmlPage htmlPage
    SingleCheckResults collector

    private Configuration myConfig

    /** executed once before all specs are executed **/
    def beforeSpec() {

    }

    /* executed before every single spec */

    def setup() {
        myConfig = new Configuration()
        brokenHttpLinksChecker = new BrokenHttpLinksChecker( myConfig )

        collector = new SingleCheckResults()
    }

    /**
     * checking for internet connectivity is a somewhat brittle - as there's no such thing as "the internet"
     * (the checker will most likely use google.com as a proxy for "internet"
     */
    // todo: test that properly
    @IgnoreIf({ Boolean.valueOf(env['INTELLIJ']) })
    def "recognize if there is internet connectivity"() {
        expect: "if there is no internet connection, testing should fail"
        NetUtil.isInternetConnectionAvailable() == true

    }

    def "empty page has no errors"() {
        given: "an empty HTML page"
        String HTML = """$HtmlConst.HTML_HEAD $HtmlConst.HTML_END """
        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "no checks are performed"
        collector.nrOfItemsChecked == 0

        and: "no error is found (aka: checkingResult is empty"
        collector.nrOfProblems() == 0

    }


    def "one syntactically correct http URL is ok"() {
        given: "an HTML page with a single correct anchor/link"
        String HTML = """$HtmlConst.HTML_HEAD 
                <a href="https://google.com">google</a>
                $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "a single item is checked"
        collector.nrOfItemsChecked == 1

        and: "the result is ok"
        collector.nrOfProblems() == 0

    }


    def "regression for issue 272"(String goodUrl) {
        given: "an HTML page with a single correct anchor/link"
        String HTML = """$HtmlConst.HTML_HEAD 
                <a href=$goodUrl>url that lead to unknown host</a>
                $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "Douglas Cayers url is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "a single item is checked"
        collector.nrOfItemsChecked == 1

        and: "the result is ok"
        collector.nrOfProblems() == 0

        where:
           goodUrl << [ "http://junit.sourceforge.net/javadoc/org/junit/Before.html",
                       "http://plumelib.org/plume-util/api/org/plumelib/util/DeterministicObject.html",
                       "http://people.csail.mit.edu/cpacheco/publications/randoop-case-study-abstract.html",
                       "https://douglascayers.com/2015/05/30/how-to-set-custom-java-path-after-installing-jdk-8/"
           ]
    }


    def "single bad link is identified as problem"() {

        given: "an HTML page with a single (bad) link"
        String badhref = "https://arc42.org/ui98jfuhenu87djch"
        String HTML = """$HtmlConst.HTML_HEAD 
                <a href=${badhref}>nonexisting arc42 link</a>
                $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "then collector contains the appropriate error message"
        collector.findings[0].whatIsTheProblem.contains(badhref)

    }

    /**
     * regression for weird behavior of certain Amazon.com links,
     * where HEAD requests are always answered with 405 instead of 200...
     */

    //@Ignore("test currently breaks. see issue-219")
    def "amazon does not deliver 405 statuscode for links that really exist"() {
        given: "an HTML page with a single (good) amazon link"
        String goodAmazonLink = "https://www.amazon.com/dp/B01A2QL9SS"
        String HTML = """$HtmlConst.HTML_HEAD 
                <a href=${goodAmazonLink}>Amazon</a>
                $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "a single item is checked"
        collector.nrOfItemsChecked == 1

        and: "the result is ok"
        collector.nrOfProblems() == 0

    }


    def "bad amazon link is identified as problem"() {

        given: "an HTML page with a single (good) amazon link"
        String badAmazonLink = "https://www.amazon.com/dp/4242424242"
        String HTML = """$HtmlConst.HTML_HEAD 
                <a href=${badAmazonLink}>Amazon</a>
                $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "then collector contains the appropriate error message"
        collector.findings[0].whatIsTheProblem.contains(badAmazonLink)

    }


    // IntelliJ has problems with testing http connections,
    // so we ignore some tests...
    @Unroll
    //@IgnoreIf({ Boolean.valueOf(env['INTELLIJ']) })
    def 'bad link #badLink is recognized as such'() {

        given: "an HTML page with a single (broken) link"
        String goodURL = "https://mock.codes/${badLink}"
        String HTML = """$HtmlConst.HTML_HEAD 
                <a href=${goodURL}>${badLink}</a>
                $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "then collector contains one error message"
        collector.getFindings().size() == 1

        where:

        badLink << [400, 401, 403, 404, 405, 406, 408, 409, 410, 429, 431, 500, 501, 502, 504, 505, 506, 507]

    }

    @Ignore
    def 'redirects are recognized and their new location is contained in warning message'() {

        given: "the old arc42 (http!) page "
        String HTML = """$HtmlConst.HTML_HEAD 
                <a href="http://arc42.de"</a>
                $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "then collector contains one error message"
        collector.getFindings().size() == 1

        collector?.getFindings()?.first().whatIsTheProblem.contains("https://arc42.de")

    }

    /**
     * guys from OpenRepose (https://github.com/rackerlabs/gradle-linkchecker-plugin/) came up with the
     * cornercase of "localhost" and "127.0.0.1"
     */
    def "urls with localhost leads to errors due to suspicious dependency to environment"() {
        // todo
        given: "HTML page with localhost url"
        String HTML = """$HtmlConst.HTML_HEAD
                         <a href="http://localhost:9001/">localhost</a>
                         $HtmlConst.HTML_END """

        htmlPage = new HtmlPage(HTML)

        when: "page is checked"
        collector = brokenHttpLinksChecker.performCheck(htmlPage)

        then: "warning is given"

        collector?.getFindings()?.first().whatIsTheProblem.contains("Warning")
        collector?.getFindings()?.first().whatIsTheProblem.contains("suspicious")

    }
}

/************************************************************************
 * This is free software - without ANY guarantee!
 *
 *
 * Copyright Dr. Gernot Starke, arc42.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *********************************************************************** */