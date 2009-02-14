import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.saar.basic.UnionFindTest;
import de.saar.chorus.domgraph.chart.SolvedFormIteratorTest;
import de.saar.chorus.domgraph.chart.SplitComputerTest;
import de.saar.chorus.domgraph.chart.modelcheck.ModelCheckTest;
import de.saar.chorus.domgraph.codec.CodecManagerTest;
import de.saar.chorus.domgraph.codec.basic.ChainTest;
import de.saar.chorus.domgraph.codec.domcon.DomconOzInputCodecTest;
import de.saar.chorus.domgraph.codec.domcon.DomconOzOutputCodecTest;
import de.saar.chorus.domgraph.codec.holesem.HolesemCodecTest;
import de.saar.chorus.domgraph.codec.mrs.MrsCodecTest;
import de.saar.chorus.domgraph.equivalence.rtg.RtgRedundancyEliminationTest;
import de.saar.chorus.domgraph.graph.CompactificationRecordTest;
import de.saar.chorus.domgraph.graph.DomGraphTest;
import de.saar.chorus.domgraph.layout.chartlayout.DomGraphChartLayoutTest;
import de.saar.chorus.domgraph.layout.domgraphlayout.DomGraphLayoutTest;
import de.saar.chorus.domgraph.weakest.WeakestReadingsComputerTest;

@RunWith(Suite.class)
@SuiteClasses(value={
        CodecManagerTest.class,
        ChainTest.class,
        DomconOzInputCodecTest.class,
        DomconOzOutputCodecTest.class,
        HolesemCodecTest.class,
        MrsCodecTest.class,
        DomGraphTest.class,
        CompactificationRecordTest.class,
        SplitComputerTest.class,
        SolvedFormIteratorTest.class,
        DomGraphChartLayoutTest.class,
        DomGraphLayoutTest.class,
        UnionFindTest.class,
        RtgRedundancyEliminationTest.class,
        WeakestReadingsComputerTest.class,
        ModelCheckTest.class
})
public class DomgraphTestSuite4 {

}
