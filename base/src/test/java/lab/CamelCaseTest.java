package lab;

import java.awt.*;
import javax.swing.*;

import java.util.logging.Level;
import org.xillium.base.beans.*;
//import org.xillium.base.etc.*;
//import org.xillium.base.*;

import org.testng.annotations.*;


/**
 * The default implementation of an object factory that creates objects from class names and arguments.
 */
public class CamelCaseTest {
    private static final String[] samples = {
        "ADD_CLBRA_FORM",
        "CACHED_DOC_INFO",
        "CHECKIN_CONFIRM",
        "CHECKIN_LIST",
        "CHECKIN_NEW_FORM",
        "CHECKIN_SEL_FORM",
        "CHECKOUT_OK",
        "CLBRA_DOCUMENT_LIST",
        "CLBRA_INFO",
        "CLBRA_LIST",
        "COMPONENT_CONFIG_TEMPLATE",
        "COMPONENT_INSTALL_FORM",
        "COMPONENT_INSTALL_LIST_FORM",
        "CONFIG_INFO",
        "CONFIG_OPTIONS_PAGE",
        "DOC_FORMATS",
        "DOC_INFO",
        "DOC_SUB_LIST",
        "DOC_TYPE_LIST",
        "EDIT_CLBRA_FORM",
        "ENV_PKG_PAGE",
        "EXECUTE_BATCH_RESULTS",
        "EXPIRED_PAGE",
        "EXTERNAL_DOC_INFO",
        "FILTER_ADMIN_PAGE",
        "FORM_PROCESS_OK",
        "FORM_SUBMIT_OK",
        "GENERIC_PAGE",
        "HOME_PAGE",
        "JAVA_PROPERTIES",
        "LISTBOX_INFO_PAGE",
        "LOCALIZATION_TEMPLATE",
        "LOCAL_REGISTRATION_FORM",
        "MSG_PAGE",
        "OPTION_LIST",
        "OUTPUT_PAGE",
        "PNE_PORTAL_DESIGN_PAGE",
        "PNE_PORTAL_PERSONAL_URLS_PAGE",
        "PREVIEW_FRAMES",
        "PREVIEW_LIST",
        "PROVIDER_LIST",
        "REDIRECT_TEMPLATE",
        "REV_HISTORY",
        "SCHEMA_VIEW_JS",
        "SELECTDOC_OK",
        "SELF_REGISTER_PROMPT_LOGIN",
        "SELF_REGISTER_USER",
        "SUBSCRIBE_FORM",
        "SUBSCRIPTION_LIST",
        "SYSTEM_AUDIT",
        "TARGETED_QUICK_SEARCH_FORM",
        "TEMPLATE_CONVERSIONS",
        "TEST_EMAIL_ENTRY_FORM",
        "UNSUBSCRIBE_FORM",
        "UPDATE_DOC_INFO",
        "USER_INFO",
        "USER_LIST",
        "WEB_APP_CONFIRM",
        "XML_HIGHLIGHT_INFO"
    };

    @Test(groups={"performance"})
    public void testSpeed() {
        String output = "";
        for (int i = 0; i < 1000; ++i) {
            output = new String(CamelCaseTest.class.getName());
        }

        long start, end;

        for (String sample: samples) {
            output = "";
            start = System.currentTimeMillis();
            for (int i = 0; i < 10000; ++i) {
                output = Beans.toCamelCase(sample, '_');
            }
            end = System.currentTimeMillis();
            System.out.println(output + ": " + (end - start));

            output = "";
            start = System.currentTimeMillis();
            for (int i = 0; i < 10000; ++i) {
                output = Beans.toLowerCamelCase(sample, '_');
            }
            end = System.currentTimeMillis();
            System.out.println(output + ": " + (end - start));
        }
    }
}
