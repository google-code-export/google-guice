/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject.testing.fieldbinder;

import static com.google.inject.Asserts.assertContains;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;
import com.google.inject.ConfigurationException;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import junit.framework.TestCase;

import java.lang.annotation.Retention;
import java.util.Arrays;
import java.util.List;

import javax.inject.Qualifier;

/** Unit tests for {@link BoundFieldModule}. */
public class BoundFieldModuleTest extends TestCase {
  public void testBindingNothing() {
    Object instance = new Object() {};

    BoundFieldModule module = BoundFieldModule.of(instance);
    Guice.createInjector(module);

    // If we didn't throw an exception, we succeeded.
  }

  public void testBindingOnePrivate() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind private Integer anInt = testValue;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
  }

  public void testBindingOnePublic() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind public Integer anInt = testValue;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
  }

  private static class FieldBindableClass {
    @Bind Integer anInt;

    FieldBindableClass(Integer anInt) {
      this.anInt = anInt;
    }
  }

  private static class FieldBindableSubclass extends FieldBindableClass {
    FieldBindableSubclass(Integer anInt) {
      super(anInt);
    }
  }

  public void testSuperTypeBinding() {
    FieldBindableSubclass instance = new FieldBindableSubclass(1024);

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(instance.anInt, injector.getInstance(Integer.class));
  }

  public void testBindingTwo() {
    final Integer testValue = 1024;
    final String testString = "Hello World!";
    Object instance = new Object() {
      @Bind private Integer anInt = testValue;
      @Bind private String aString = testString;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
    assertEquals(testString, injector.getInstance(String.class));
  }

  public void testBindingSuperType() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind(to = Number.class) private Integer anInt = testValue;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Number.class));
  }

  public void testBindingSuperTypeAccessSubType() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind(to = Number.class) private Integer anInt = testValue;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    try {
      injector.getInstance(Integer.class);
    } catch (ConfigurationException e) {
      assertContains(
          e.getMessage(),
          "Could not find a suitable constructor in java.lang.Integer");
    }
  }

  public void testBindingIncorrectTypeProviderFails() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind(to = String.class) private Provider<Integer> anIntProvider = new Provider<Integer>() {
        @Override public Integer get() {
          return testValue;
        }
      };
    };

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
      fail();
    } catch (CreationException e) {
      assertContains(
          e.getMessage(),
          "Requested binding type \"java.lang.String\" is not "
          + "assignable from field binding type \"java.lang.Integer\"");
    }
  }

  @BindingAnnotation
  @Retention(RUNTIME)
  private static @interface SomeBindingAnnotation {}

  public void testBindingWithBindingAnnotation() {
    final Integer testValue1 = 1024, testValue2 = 2048;
    Object instance = new Object() {
      @Bind private Integer anInt = testValue1;

      @Bind
      @SomeBindingAnnotation
      private Integer anotherInt = testValue2;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue1, injector.getInstance(Integer.class));
    assertEquals(
        testValue2,
        injector.getInstance(Key.get(Integer.class, SomeBindingAnnotation.class)));
  }

  @Qualifier
  @Retention(RUNTIME)
  private static @interface SomeQualifier {}

  public void testBindingWithQualifier() {
    final Integer testValue1 = 1024, testValue2 = 2048;
    Object instance = new Object() {
      @Bind private Integer anInt = testValue1;

      @Bind
      @SomeQualifier
      private Integer anotherInt = testValue2;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue1, injector.getInstance(Integer.class));
    assertEquals(
        testValue2,
        injector.getInstance(Key.get(Integer.class, SomeQualifier.class)));
  }

  public void testCanReuseBindingAnnotationsWithDifferentValues() {
    final Integer testValue1 = 1024, testValue2 = 2048;
    final String name1 = "foo", name2 = "bar";
    Object instance = new Object() {
      @Bind
      @Named(name1)
      private Integer anInt = testValue1;

      @Bind
      @Named(name2)
      private Integer anotherInt = testValue2;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(
        testValue1,
        injector.getInstance(Key.get(Integer.class, Names.named(name1))));
    assertEquals(
        testValue2,
        injector.getInstance(Key.get(Integer.class, Names.named(name2))));
  }

  public void testBindingWithValuedBindingAnnotation() {
    final Integer testValue1 = 1024, testValue2 = 2048;
    final String name = "foo";
    Object instance = new Object() {
      @Bind private Integer anInt = testValue1;

      @Bind
      @Named(name)
      private Integer anotherInt = testValue2;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue1, injector.getInstance(Integer.class));
    assertEquals(
        testValue2,
        injector.getInstance(Key.get(Integer.class, Names.named(name))));
  }

  public void testBindingWithGenerics() {
    final List<Integer> testIntList = Arrays.asList(new Integer[] {1, 2, 3});
    final List<Boolean> testBoolList = Arrays.asList(new Boolean[] {true, true, false});
    Object instance = new Object() {
      @Bind private List<Integer> anIntList = testIntList;
      @Bind private List<Boolean> aBoolList = testBoolList;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testIntList, injector.getInstance(new Key<List<Integer>>() {}));
    assertEquals(testBoolList, injector.getInstance(new Key<List<Boolean>>() {}));
  }

  public void testBoundValueDoesntChange() {
    Integer testValue = 1024;
    FieldBindableClass instance = new FieldBindableClass(testValue);

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
    instance.anInt++;
    assertEquals(testValue, injector.getInstance(Integer.class));
  }

  public void testIncompatibleBindingType() {
    final Integer testInt = 1024;
    Object instance = new Object() {
      @Bind(to = String.class) private Integer anInt = testInt;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
      fail();
    } catch (CreationException e) {
      assertContains(e.getMessage(),
          "Requested binding type \"java.lang.String\" is not assignable from field binding type "
          + "\"java.lang.Integer\"");
    }
  }

  public void testFailureOnMultipleBindingAnnotations() {
    final Integer testInt = 1024;
    Object instance = new Object() {
      @Bind
      @Named("a")
      @SomeBindingAnnotation
      private Integer anInt = testInt;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
    } catch (CreationException e) {
      assertContains(e.getMessage(), "More than one annotation is specified for this binding.");
    }
  }

  public void testBindingSuperTypeAndBindingAnnotation() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind(to = Number.class)
      @Named("foo")
      private Integer anInt = testValue;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Key.get(Number.class, Names.named("foo"))));
  }

  public void testBindingProvider() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind private Provider<Integer> anInt = new Provider<Integer>() {
        @Override public Integer get() {
          return testValue;
        }
      };
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
  }

  public void testBindingNullField() {
    Object instance = new Object() {
      @Bind private Integer anInt = null;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
      fail();
    } catch (CreationException e) {
      assertContains(e.getMessage(),
          "Binding to null values is not allowed. "
          + "Use Providers.of(null) if this is your intended behavior.");
    }
  }

  public void testBindingNullProvider() {
    Object instance = new Object() {
      @Bind private Provider<Integer> anIntProvider = null;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
      fail();
    } catch (CreationException e) {
      assertContains(e.getMessage(),
          "Binding to null values is not allowed. "
          + "Use Providers.of(null) if this is your intended behavior.");
    }
  }

  private static class IntegerProvider implements Provider<Integer> {
    private final Integer value;

    IntegerProvider(Integer value) {
      this.value = value;
    }

    @Override public Integer get() {
      return value;
    }
  }

  public void testProviderSubclassesBindToTheProviderItself() {
    final IntegerProvider integerProvider = new IntegerProvider(1024);
    Object instance = new Object() {
      @Bind private IntegerProvider anIntProvider = integerProvider;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(integerProvider, injector.getInstance(IntegerProvider.class));
  }

  public void testProviderSubclassesDoNotBindParameterizedType() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind private IntegerProvider anIntProvider = new IntegerProvider(testValue);
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    try {
      injector.getInstance(Integer.class);
      fail();
    } catch (ConfigurationException e) {
      assertContains(e.getMessage(), "Could not find a suitable constructor in java.lang.Integer.");
    }
  }

  private static class ParameterizedObject<T> {
    ParameterizedObject(T instance) {
      this.instance = instance;
    }

    @Bind private T instance;
  }

  public void testBindParameterizedTypeFails() {
    ParameterizedObject<Integer> instance = new ParameterizedObject<Integer>(0);

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
      fail();
    } catch (CreationException e) {
      assertContains(e.getMessage(), "T cannot be used as a key; It is not fully specified.");
    }
  }

  public void testBindSubclassOfParameterizedTypeSucceeds() {
    final Integer testValue = 1024;
    ParameterizedObject<Integer> instance = new ParameterizedObject<Integer>(testValue) {};

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
  }

  public void testBindArray() {
    final Integer[] testArray = new Integer[] { 1024, 2048 };
    Object instance = new Object() {
      @Bind private Integer[] anIntArray = testArray;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testArray, injector.getInstance(Integer[].class));
  }

  public void testRawProviderCannotBeBound() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind private Provider anIntProvider = new Provider() {
        @Override public Object get() {
          return testValue;
        }
      };
    };

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
      fail();
    } catch (CreationException e) {
      assertContains(e.getMessage(),
          "Non parameterized Provider fields must have an "
          + "explicit binding class via @Bind(to = Foo.class)");
    }
  }

  public void testExplicitlyBoundRawProviderCanBeBound() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind(to = Integer.class) private Provider anIntProvider = new Provider() {
        @Override public Object get() {
          return testValue;
        }
      };
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
  }

  public void testRawProviderCanBindToIncorrectType() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind(to = String.class) private Provider anIntProvider = new Provider() {
        @Override public Object get() {
          return testValue;
        }
      };
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(String.class));
  }

  public void testMultipleErrorsAreAggregated() {
    Object instance = new Object() {
      @Bind private Provider aProvider;
      @Bind(to = String.class) private Integer anInt;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    try {
      Guice.createInjector(module);
    } catch (CreationException e) {
      assertEquals(2, e.getErrorMessages().size());
    }
  }

  public void testBindingProviderWithProviderSubclassValue() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind private Provider<Integer> anIntProvider = new IntegerProvider(testValue);
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Integer.class));
  }

  public void testBoundFieldsCannotBeInjected() {
    Object instance = new Object() {
      @Bind
      @Inject
      Integer anInt = 0;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
    } catch (CreationException e) {
      assertContains(
          e.getMessage(),
          "Fields annotated with both @Bind and @Inject are illegal.");
    }
  }

  public void testIncrementingProvider() {
    final Integer testBaseValue = 1024;
    Object instance = new Object() {
      @Bind private Provider<Integer> anIntProvider = new Provider<Integer>() {
        private int value = testBaseValue;

        @Override public Integer get() {
          return value++;
        }
      };
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testBaseValue, injector.getInstance(Integer.class));
    assertEquals((Integer) (testBaseValue + 1), injector.getInstance(Integer.class));
    assertEquals((Integer) (testBaseValue + 2), injector.getInstance(Integer.class));
  }

  public void testProviderDoesNotProvideDuringInjectorConstruction() {
    Object instance = new Object() {
      @Bind private Provider<Integer> myIntProvider = new Provider<Integer>() {
        @Override public Integer get() {
          throw new UnsupportedOperationException();
        }
      };
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Guice.createInjector(module);

    // If we don't throw an exception, we succeeded.
  }

  private static class InvalidBindableClass {
    @Bind(to = String.class) Integer anInt;
  }

  public void testIncompatibleBindingTypeStackTraceHasUserFrame() {
    Object instance = new InvalidBindableClass();

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
      fail();
    } catch (CreationException e) {
      assertContains(e.getMessage(), "at " + InvalidBindableClass.class.getName() + " field anInt");
    }
  }

  private static class InjectedNumberProvider implements Provider<Number> {
    @Inject Integer anInt;

    @Override public Number get() {
      return anInt;
    }
  }

  public void testBoundProvidersAreInjected() {
    final Integer testValue = 1024;
    Object instance = new Object() {
      @Bind private Integer anInt = testValue;
      @Bind private Provider<Number> aNumberProvider = new InjectedNumberProvider();
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue, injector.getInstance(Number.class));
  }

  public void testBoundInstancesAreInjected() {
    final Integer testValue = 1024;
    final InjectedNumberProvider testNumberProvider = new InjectedNumberProvider();
    Object instance = new Object() {
      @Bind private Integer anInt = testValue;
      @Bind private InjectedNumberProvider aNumberProvider = testNumberProvider;
    };

    BoundFieldModule module = BoundFieldModule.of(instance);
    Guice.createInjector(module);

    assertEquals(testValue, testNumberProvider.anInt);
  }

  private static class InvalidBindableSubclass extends InvalidBindableClass {}

  public void testClassIsPrintedInErrorsWhenCauseIsSuperclass() {
    Object instance = new InvalidBindableSubclass();

    BoundFieldModule module = BoundFieldModule.of(instance);

    try {
      Guice.createInjector(module);
    } catch (CreationException e) {
      assertContains(
          e.getMessage(),
          "Requested binding type \"java.lang.String\" is not assignable from field binding type "
          + "\"java.lang.Integer\"");
    }
  }

  private static class FieldBindableSubclass2 extends FieldBindableClass {
    @Bind Number aNumber;

    FieldBindableSubclass2(Integer anInt, Number aNumber) {
      super(anInt);
      this.aNumber = aNumber;
    }
  }

  public void testFieldsAreBoundFromFullClassHierarchy() {
    final Integer testValue1 = 1024, testValue2 = 2048;
    FieldBindableSubclass2 instance = new FieldBindableSubclass2(testValue1, testValue2);

    BoundFieldModule module = BoundFieldModule.of(instance);
    Injector injector = Guice.createInjector(module);

    assertEquals(testValue1, injector.getInstance(Integer.class));
    assertEquals(testValue2, injector.getInstance(Number.class));
  }
}
