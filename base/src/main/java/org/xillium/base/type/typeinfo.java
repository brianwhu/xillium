package org.xillium.base.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotation to retain type information on generics otherwise eradicated by type erasure.
 * Typical uses include
 * <pre>
 *      {@code @typeinfo(MyEnum.class) Flags<MyEnum> flags;}
 *      {@code @typeinfo({Type1.class, Type2.class}) MyGeneric<Type1, Type2> f1;}
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface typeinfo {
    Class<?>[] value() default { Object.class };
}
