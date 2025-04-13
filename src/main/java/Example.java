import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

public class Example {
    public static void main(String[] args) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("http://localhost:4200/base/applist");
            page.getByTestId("start-add").click();
            page.getByTestId("add-new-pipeline").click();
            page.getByPlaceholder("MySQL-import").click();
            page.getByPlaceholder("MySQL-import").fill("baisui4");
            page.locator("nz-form-control").filter(new Locator.FilterOptions().setHasText("部门管理")).getByRole(AriaRole.TEXTBOX).click();
            page.getByText("/2dfire/Engineering department").click();
            page.getByPlaceholder("小明").click();
            page.getByPlaceholder("小明").fill("小明");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("下一步")).click();
            page.locator("#source_mysql").click();
            page.locator("#sink_mysql").click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("下一步")).click();


            // source mysql 页面设置
            page.getByTestId("dbName").getByRole(AriaRole.TEXTBOX).click();
            page.getByTestId("dbName").getByRole(AriaRole.TEXTBOX).fill("order");
            page.getByText("order", new Page.GetByTextOptions().setExact(true)).click();
           // page.locator("#cdk-overlay-3").getByText("order", new Locator.GetByTextOptions().setExact(true)).click();
            page.getByTestId("next-step").click();

            page.pause();

            page.locator("nz-transfer-list").filter(new Locator.FilterOptions().setHasText("40 项 表名 Reload totalpayinfowaitinginstanceinfoorder_refundai_brandorder_tagpayde")).getByPlaceholder("请输入搜索内容").click();
            page.locator("nz-transfer-list").filter(new Locator.FilterOptions().setHasText("40 项 表名 Reload totalpayinfowaitinginstanceinfoorder_refundai_brandorder_tagpayde")).getByPlaceholder("请输入搜索内容").fill("wait");
            page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("waitingorderdetail")).getByLabel("").check();
            page.locator(".ant-transfer-operation > button:nth-child(2)").click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("批量设置")).click();
            page.getByTestId("next-step").click();
            page.getByTestId("dbName").getByRole(AriaRole.TEXTBOX).click();
            page.getByTestId("dbName").getByRole(AriaRole.TEXTBOX).fill("shop");
            page.getByText("shop").click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("精简")).click();
            page.getByTestId("aliasPrefix").click();
            page.getByTestId("aliasPrefix").fill("ods_");
            page.getByPlaceholder("delete from @table").click();
            page.getByPlaceholder("delete from @table").fill("delete from @table");
            page.getByTestId("next-step").click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("重置")).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("执行")).click();
            page.getByTestId("next-step").click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("创建")).click();

            page.pause();
            //  Thread.sleep(99000);
        }
    }
}