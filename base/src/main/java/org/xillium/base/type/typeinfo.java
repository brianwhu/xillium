package org.xillium.base.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Annotation to retain type information on generics otherwise eradicated by type erasure.
 * Typical uses include
 * <xmp>
 *  @typeinfo(MyEnum.class) Flags<MyEnum> flags;
 *  @typeinfo({Type1.class, Type2.class}) MyGeneric<Type1, Type2> f1;
 * </xmp>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface typeinfo {
    Class<?>[] value() default { Object.class };
}
