package org.picocontainer.containers;

import org.junit.Test;
import org.picocontainer.DefaultPicoContainer;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.annotations.Bind;
import org.picocontainer.injectors.AbstractInjector;
import org.picocontainer.testmodel.DependsOnTouchable;
import org.picocontainer.testmodel.SimpleTouchable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.picocontainer.Key.annotatedKey;
import java.util.List;

public class TieringPicoContainerTestCase {

    public static class Couch {
    }

    public static class TiredPerson {
        private Couch couchToSitOn;

        public TiredPerson(Couch couchToSitOn) {
            this.couchToSitOn = couchToSitOn;
        }
    }

    @Test
    public void testThatGrandparentTraversalForComponentsCanBeBlocked() {
        MutablePicoContainer grandparent = new TieringPicoContainer();
        MutablePicoContainer parent = grandparent.makeChildContainer();
        MutablePicoContainer child = parent.makeChildContainer();
        grandparent.addComponent(Couch.class);
        child.addComponent(TiredPerson.class);

        TiredPerson tp = null;
        try {
            tp = child.getComponent(TiredPerson.class);
            fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            // expected
        }

    }

    @Test
    public void testThatParentTraversalIsOkForTiering() {
        MutablePicoContainer parent = new TieringPicoContainer();
        MutablePicoContainer child = parent.makeChildContainer();
        parent.addComponent(Couch.class);
        child.addComponent(TiredPerson.class);

        TiredPerson tp = child.getComponent(TiredPerson.class);
        assertNotNull(tp);
        assertNotNull(tp.couchToSitOn);

    }

    public static class Doctor {
        private TiredPerson tiredPerson;

        public Doctor(TiredPerson tiredPerson) {
            this.tiredPerson = tiredPerson;
        }
    }

    public static class TiredDoctor {
        private Couch couchToSitOn;
        private final TiredPerson tiredPerson;

        public TiredDoctor(Couch couchToSitOn, TiredPerson tiredPerson) {
            this.couchToSitOn = couchToSitOn;
            this.tiredPerson = tiredPerson;
        }
    }

    @Test
    public void testThatParentTraversalIsOnlyBlockedOneTierAtATime() {
        MutablePicoContainer gp = new TieringPicoContainer();
        MutablePicoContainer p = gp.makeChildContainer();
        MutablePicoContainer c = p.makeChildContainer();
        gp.addComponent(Couch.class);
        p.addComponent(TiredPerson.class);
        c.addComponent(Doctor.class);
        c.addComponent(TiredDoctor.class);
        Doctor d = c.getComponent(Doctor.class);
        assertNotNull(d);
        assertNotNull(d.tiredPerson);
        assertNotNull(d.tiredPerson.couchToSitOn);
        try {
            TiredDoctor td = c.getComponent(TiredDoctor.class);
            fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            // expected
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Bind
    public static @interface Grouchy {}

    public static class GrouchyTiredPerson extends TiredPerson {
        public GrouchyTiredPerson(Couch couchToSitOn) {
            super(couchToSitOn);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Bind
    public static @interface Polite {}

    public static class PoliteTiredPerson extends TiredPerson {
        public PoliteTiredPerson(Couch couchToSitOn) {
            super(couchToSitOn);
        }
    }

    public static class DiscerningDoctor {
        private final TiredPerson tiredPerson;

        public DiscerningDoctor(@Polite TiredPerson tiredPerson) {
            this.tiredPerson = tiredPerson;
        }
    }

    @Test
    public void testThatGrandparentTraversalForComponentsCanBeBlockedEvenForAnnotatedInjections() {
        MutablePicoContainer grandparent = new TieringPicoContainer();
        MutablePicoContainer parent = grandparent.makeChildContainer();
        MutablePicoContainer child = parent.makeChildContainer();
        grandparent.addComponent(Couch.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Polite.class), PoliteTiredPerson.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Grouchy.class), GrouchyTiredPerson.class);
        child.addComponent(DiscerningDoctor.class);

        assertNotNull(grandparent.getComponent(TiredPerson.class, Polite.class));
        assertNotNull(grandparent.getComponent(TiredPerson.class, Grouchy.class));

        DiscerningDoctor dd = null;
        try {
            dd = child.getComponent(DiscerningDoctor.class);
            fail("should have barfed");
        } catch (AbstractInjector.UnsatisfiableDependenciesException e) {
            // expected
        }

    }

    @Test
    public void testThatGrandparentTraversalForComponentsCanBeBlockedEvenForAnnotatedInjections2() {
        MutablePicoContainer grandparent = new TieringPicoContainer();
        grandparent.addComponent(Couch.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Polite.class), PoliteTiredPerson.class);
        grandparent.addComponent(annotatedKey(TiredPerson.class, Grouchy.class), GrouchyTiredPerson.class);
        grandparent.addComponent(DiscerningDoctor.class);

        assertNotNull(grandparent.getComponent(TiredPerson.class, Polite.class));
        assertNotNull(grandparent.getComponent(TiredPerson.class, Grouchy.class));

        DiscerningDoctor dd = grandparent.getComponent(DiscerningDoctor.class);
        assertNotNull(dd.tiredPerson);
        assertTrue(dd.tiredPerson instanceof PoliteTiredPerson);

    }

    @Test public void testRepresentationOfContainerTree() {
		TieringPicoContainer parent = new TieringPicoContainer();
        parent.setName("parent");
        TieringPicoContainer child = new TieringPicoContainer(parent);
        child.setName("child");
		parent.addComponent("st", SimpleTouchable.class);
		child.addComponent("dot", DependsOnTouchable.class);
        assertEquals("child:1<I<parent:1<|", child.toString());
    }

    public static class A { public A(B b) { this.b = b; } public B b; }
    public static class B { public B(String name) { this.name = name; } public String name; }

    @Test public void testScopeResolution() {

    	DefaultPicoContainer container = new DefaultPicoContainer();
    	container.addComponent(A.class);
    	container.addComponent(new B("root"));

    	MutablePicoContainer scope = container.makeChildContainer();
    	scope.addComponent(new B("child"));

    	A a = scope.getComponent(A.class);

    	assertEquals("child", a.b.name);
    }

    public static interface I1 {}
    public static class C1 implements I1 {}
    public static interface I2 extends I1 {}
    public static class C2 extends C1 implements I1 {}
    public static class C3 {
        public C3(I1 i1) {}
      }

      @Test public void testAmbigousInterfaceResolution() {

      	DefaultPicoContainer container = new DefaultPicoContainer();
      	container.addComponent(C1.class);
      	container.addComponent(C2.class);

      	I1 i = container.getComponent(I1.class);

      	assertEquals(C2.class, i.getClass());
      }

      @Test public void testAmbigousConstructorResolution() {

      	DefaultPicoContainer container = new DefaultPicoContainer();
      	container.addComponent(C1.class);
      	container.addComponent(C2.class);
      	container.addComponent(C3.class);

      	C3 c3 = container.getComponent(C3.class);

      	assertEquals(C3.class, c3.getClass());
      }
      
      @Test public void testScopeArrayResolution() {

      	DefaultPicoContainer container = new DefaultPicoContainer();
      	container.addComponent(A.class);
      	container.addComponent(new B("root"));

      	final List<A> a1 = container.getComponents(A.class);
      	assertEquals(1, a1.size());

      	MutablePicoContainer scope = container.makeChildContainer();
      	scope.addComponent(new B("child"));

      	final List<A> a2 = scope.getComponents(A.class);

      	assertEquals(1, a2.size());
      	assertEquals("child", a2.get(0).b.name);
      }      
}
