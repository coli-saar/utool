package de.saar.chorus.domgraph.utool.server

import de.saar.basic.Logger
import org.junit.Before
import org.junit.Test

import java.net.ServerSocket
import java.net.Socket

/** End-to-end coverage for filtering through the XML server protocol. */
class ServerFilteringTest {
    private static final String GRAPH =
            "[label(x1 a(x2 x3)) label(y1 a(y2 y3)) " +
            "label(z1 foo) label(z2 bar) label(z3 baz) " +
            "dom(x2 z1) dom(y2 z2) dom(x3 z3) dom(y3 z3)]"

    @Before
    void clearCachedRules() {
        def field = XmlParser.getDeclaredField("previousRnfc")
        field.accessible = true
        field.set(null, null)
    }

    @Test
    void rejectsCacheReuseBeforeRulesWereLoaded() {
        def xml = new groovy.util.XmlSlurper().parseText(
                sendRequest(solvableRequest("<filter/>")))

        assert xml.name() == "error"
        assert xml.@code.text() == "170"
        assert xml.@explanation.text().contains("without specifying the rules")
    }

    @Test
    void filtersSolvableAndSolveResultsAndReusesCachedRules() {
        def unfiltered = parseResult(sendRequest(solvableRequest("")))
        assert unfiltered.@solvable.text() == "true"
        assert unfiltered.@count.text() == "2"

        String rules = getClass().getResource("/server/filter-rules.txt").getText("UTF-8")
        String filterWithRules = "<filter rules=\"${xmlAttribute(rules)}\"/>"

        def filtered = parseResult(sendRequest(solvableRequest(filterWithRules)))
        assert filtered.@solvable.text() == "true"
        assert filtered.@count.text() == "1"

        // XmlParser caches the last RNF computer process-wide. A new connection
        // can therefore request the same filtering with an empty filter element.
        def cached = parseResult(sendRequest(solvableRequest("<filter/>")))
        assert cached.@solvable.text() == "true"
        assert cached.@count.text() == "1"

        // The solve operation must enumerate the reduced chart as well.
        def solved = parseResult(sendRequest(solveRequest("<filter/>")))
        assert solved.@solvable.text() == "true"
        assert solved.@count.text() == "1"
        assert solved.solution.size() == 0 // no output codec means no-output mode
    }

    private static String solvableRequest(String filter) {
        """<utool cmd="solvable">
  <usr codec="domcon-oz" string="${GRAPH}"/>
  ${filter}
</utool>
"""
    }

    private static String solveRequest(String filter) {
        """<utool cmd="solve">
  <usr codec="domcon-oz" string="${GRAPH}"/>
  ${filter}
</utool>
"""
    }

    private static String xmlAttribute(String value) {
        value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
    }

    private static def parseResult(String response) {
        def xml = new groovy.util.XmlSlurper().parseText(response)
        assert xml.name() == "result" : response
        xml
    }

    private static String sendRequest(String request) {
        ServerSocket listener = new ServerSocket(0)
        Socket client = null
        Socket accepted = null
        ServerThread serverThread = null

        try {
            client = new Socket("127.0.0.1", listener.localPort)
            accepted = listener.accept()
            listener.close()

            serverThread = new ServerThread(accepted, new Logger(false, null))
            serverThread.start()

            Writer writer = new OutputStreamWriter(client.outputStream, "UTF-8")
            writer.write(request)
            writer.flush()
            client.shutdownOutput()

            String response = client.inputStream.getText("UTF-8")
            serverThread.join(5000)
            assert !serverThread.isAlive() : "Server thread did not terminate"
            response
        } finally {
            if (!listener.isClosed()) {
                listener.close()
            }
            if (client != null && !client.isClosed()) {
                client.close()
            }
            if (accepted != null && !accepted.isClosed()) {
                accepted.close()
            }
        }
    }
}
