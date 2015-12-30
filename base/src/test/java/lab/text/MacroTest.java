package lab.text;

import java.util.*;
import javax.xml.bind.DatatypeConverter;
import org.xillium.base.Open;
import org.xillium.base.beans.*;
import org.xillium.base.text.Macro;

import org.testng.annotations.*;


/**
 * Macro test cases
 */
public class MacroTest {
    public static class Event implements Open {
        public String title;
        public String date;
        public String location;
        public String content;

        public Event(String t, String d, String l, String c) {
            title = t;
            date = d;
            location = l;
            content = c;
        }
    }

    public static class Discount implements Open {
        public static class Product implements Open {
            public String name;
            public String time;
            public String options[];

            public Product(String n, String d, String... o) {
                name = n;
                time = d;
                options = o;
            }
        }

        public String program;
        public String content;
        public List<Product> products = new ArrayList<>();
        public String quality = "quick";

        public Discount(String t, String c) {
            program = t;
            content = c;
        }

        public Discount add(Product p) {
            products.add(p);
            return this;
        }
    }

    public static class Values implements Open {
        public Event event[] = {
            new Event("Event-1", "Date-1", "Location-1", "Content-1"),
            new Event("Event-2", "Date-2", "Location-2", "Content-2"),
            new Event("Event-3", "Date-3", "Location-3", "Content-3")
        };
        public List<Discount> discount = Arrays.asList(
            new Discount("Program-1", "Discount-1"),
            new Discount("Program-2", "Discount-2"),
            new Discount("Program-3", "Discount-3"),
            new Discount("Program-4", "Discount-4")
            .add(new Discount.Product("Name-1", "Time-1", "Option-1", "Option-2", "Option-3"))
            .add(new Discount.Product("Name-2", "Time-2", "Option-4", "Option-5", "Option-6"))
            .add(new Discount.Product("Name-3", "Time-3", "Option-7", "Option-8", "Option-9"))
        );
        public List<String> quotes = Arrays.asList(
            "Quote-1",
            "Quote-2",
            "Quote-3",
            null,
            "Quote-5"
        );
    }

    @Test(groups={"functional", "text"})
    public void testTranslation() {
        Map<String, String> res = new HashMap<>();
        res.put("text/document", "<h1>New Events:</h1>\n{@event@}\n<h1>Discounts:</h1>\n{@discount@}\n<h1>Quotes:</h1>\n{@quote:quotes@}\n{@footer(new)@}");
        res.put("text/event", "<p><em>{title}</em> - <i>{location}</i> {content}</p>\n");
        res.put("text/discount", "<p><em>{program}</em> - {content} {{@prefix@}@product-{quality}:products@{@suffix@}}</p>\n");
        res.put("text/product-full", "<li><em>{name}</em> - {time}{@option:options@}</li>\n");
        res.put("text/product-quick", "<li>{name} - {time}{@option:options@}</li>\n");
        res.put("text/option", ", {value}");
        res.put("text/quote", "<p>{value}</p>\n");
        res.put("text/footer", "<h4>Thank you for visiting our {1} site, {username:-our valued customer}!</h4>\n");
        res.put("text/prefix", "<ul>\n");
        res.put("text/suffix", "</ul>\n");

        System.out.println(Macro.expand(res, "text/document", new Values()));
    }
}
