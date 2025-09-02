package hudson.plugins.filesystem_scm;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleAntWildcardFilterTest {

    @TempDir
    private File tmpDir;

    @Test
    void test1() throws Exception {
        File myDir = new File(tmpDir, "abc001234/def/xyz.000");
        myDir.mkdirs();
        File f1 = new File(myDir, "aa.txt");
        f1.createNewFile();
        File f2 = new File(myDir, "bb.111");
        f2.createNewFile();
        File f3 = new File(myDir.getParentFile(), "ab.tx1");
        f3.createNewFile();

        IOFileFilter iof = new SimpleAntWildcardFilter(tmpDir.getAbsolutePath() + "/**/a?.tx?");
        File checkDir = new File(tmpDir, "abc001234");
        Collection<File> coll = FileUtils.listFiles(checkDir, iof, HiddenFileFilter.VISIBLE);
        assertEquals(2, coll.size());
    }

    @Test
    void test2() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/CVS/*");
        assertEquals("(/.*)?/CVS/[^\\/]*$", filter.getPattern().toString());
    }

    @Test
    void test3() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("org/apache/jakarta/**");
        assertEquals("/org/apache/jakarta(/.*)?$", filter.getPattern().toString());
    }

    @Test
    void test4() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/*.gif");
        assertEquals("(/.*)?/[^\\/]*\\.gif$", filter.getPattern().toString());
    }

    @Test
    void test5() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("*/.hgignore");
        assertEquals("/[^\\/]*/\\.hgignore$", filter.getPattern().toString());
    }

    @Test
    void test6() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("/views/**/*.cfm");
        assertEquals("^/views(/.*)?/[^\\/]*\\.cfm$", filter.getPattern().toString());
    }

    @Test
    void test7() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("/views/index??.cfm");
        assertEquals("^/views/index..\\.cfm$", filter.getPattern().toString());
    }

    @Test
    void test8() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("WEB-INF/*-context.xml");
        assertEquals("/WEB-INF/[^\\/]*-context\\.xml$", filter.getPattern().toString());
    }

    @Test
    void test9() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("C:/some/path/*-context.xml");
        assertEquals("^C:/some/path/[^\\/]*-context\\.xml$", filter.getPattern().toString());
    }

    @Test
    void test10() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/abc/def.txt");
        assertEquals("(/.*)?/abc/def\\.txt$", filter.getPattern().toString());
    }

    @Test
    void test11() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("hello/ab/**");
        assertEquals("/hello/ab(/.*)?$", filter.getPattern().toString());
    }

    @Test
    void test12() {
        SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter("**/**/a/b/**/");
        assertEquals("(/.*)?(/.*)?/a/b(/.*)?$", filter.getPattern().toString());
    }
}
