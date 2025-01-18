package tools;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FindInfoInWeb {

    //llvm: "https://github.com/llvm/llvm-project/issues?page=" + i + "&q=label%3Amiscompilation+is%3Aclosed"
    private static final String GITHUB_URL = "https://gcc.gnu.org/bugzilla/buglist.cgi?bug_status=RESOLVED&bug_status=VERIFIED&bug_status=CLOSED&cf_known_to_fail_type=allwords&cf_known_to_work_type=allwords&component=tree-optimization&product=gcc&query_format=advanced&resolution=FIXED&resolution=DUPLICATE";
//    private static final String CLANG_PATTERN = "(clang\\s+|clang-tk\\s+|opt\\s+|-O\\d+|-Os)";  // 匹配 clang, opt, -O 或 -Os 的模式
    private static final String GCC_PATTERN = "(gcc\\s+|gcc-tk\\s+|-O\\d+|-Os)";

    public static void main(String[] args) {
        try {
            // 1. 获取 GitHub 页面内容
            Document doc = Jsoup.connect(GITHUB_URL).timeout(10000).get();
            // 2. 选择所有包含 Issue 链接的元素
            Elements issueLinks = doc.select("a[href*=\"show_bug.cgi\"]");  // 查找所有 Issue 链接
            System.out.println(issueLinks.size());

            for (Element link : issueLinks) {
                String issueUrl = link.absUrl("href");  // 获取绝对 URL
                System.out.println("Processing issue: " + issueUrl);
                processIssuePage(issueUrl);  // 处理每个 Issue 页面
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void processIssuePage(String issueUrl) {
        try {
            // 3. 获取每个 Issue 页面的内容
            Document issueDoc = Jsoup.connect(issueUrl).get();
            String issueContent = issueDoc.body().text();  // 获取页面正文文本

            // 4. 按行拆分内容
            String[] lines = issueContent.split("\n");

            // 5. 使用正则表达式查找 clang, opt, -O 或 -Os 指令
            Pattern pattern = Pattern.compile(GCC_PATTERN);
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find() && line.contains(".c") && line.contains("-march=")) {
                    System.out.println(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
