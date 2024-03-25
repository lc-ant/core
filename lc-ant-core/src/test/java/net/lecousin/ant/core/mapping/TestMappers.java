package net.lecousin.ant.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TestMappers {
	
	public static class PrimitiveInt {
		public int a;
	}

	public static class PrimitiveLong {
		public long a;
	}
	
	public static class WrapperInt {
		public Integer a;
	}

	public static class WrapperLong {
		public Long a;
	}


	public static class OptionalRaw {
		public Optional a;
	}
	
	public static class OptionalLong {
		public Optional<Long> a;
	}

	
	public static class ListRaw {
		public List a;
	}
	
	public static class ListInteger {
		public List<Integer> a;
	}
	
	public static class ListLong {
		public List<Long> a;
	}
	
	public static class ListOptionalLong {
		public List<Optional<Long>> a;
	}
	
	
	public static class ArrayInt {
		public int[] a;
	}
	
	public static class ArrayOptionalLong {
		public Optional<Long>[] a;
	}
	
	
	public static class WithInstant {
		public Instant a;
	}
	
	public static class WithLocalDate {
		public LocalDate a;
	}
	
	
	@Test
	void testPrimitiveIntToPrimitiveLong() {
		PrimitiveInt pi = new PrimitiveInt();
		pi.a = 23;
		PrimitiveLong pl = Mappers.map(pi, PrimitiveLong.class);
		assertThat(pl).isNotNull();
		assertThat(pl.a).isEqualTo(23L);
	}
	
	@Test
	void testPrimitiveIntToWrapperLong() {
		PrimitiveInt pi = new PrimitiveInt();
		pi.a = 23;
		WrapperLong wl = Mappers.map(pi, WrapperLong.class);
		assertThat(wl).isNotNull();
		assertThat(wl.a).isEqualTo(23L);
	}
	
	@Test
	void testOptionalLongToOptionalRaw() {
		OptionalLong ol = new OptionalLong();
		ol.a = Optional.of(15L);
		OptionalRaw or = Mappers.map(ol, OptionalRaw.class);
		assertThat(or).isNotNull();
		assertThat(or.a).isPresent().hasValue(15L);
	}
	
	@Test
	void testOptionalLongToPrimitiveInt() {
		OptionalLong ol = new OptionalLong();
		ol.a = Optional.of(15L);
		PrimitiveInt pi = Mappers.map(ol, PrimitiveInt.class);
		assertThat(pi).isNotNull();
		assertThat(pi.a).isEqualTo(15);
	}
	
	@Test
	void testOptionalLongToWrapperInt() {
		OptionalLong ol = new OptionalLong();
		ol.a = Optional.of(15L);
		WrapperInt wi = Mappers.map(ol, WrapperInt.class);
		assertThat(wi).isNotNull();
		assertThat(wi.a).isEqualTo(15);
	}
	
	@Test
	void testListRawToListInteger() {
		ListRaw lr = new ListRaw();
		lr.a = List.of(10, 20, 30);
		ListInteger li = Mappers.map(lr, ListInteger.class);
		assertThat(li).isNotNull();
		assertThat(li.a).isNotNull();
		assertThat(li.a).containsExactly(10, 20, 30);
	}
	
	@Test
	void testListRawToListLong() {
		ListRaw lr = new ListRaw();
		lr.a = List.of(10, 20, 30);
		ListLong ll = Mappers.map(lr, ListLong.class);
		assertThat(ll).isNotNull();
		assertThat(ll.a).isNotNull();
		assertThat(ll.a).containsExactly(10L, 20L, 30L);
	}
	
	@Test
	void testListRawToListOptionalLong() {
		ListRaw lr = new ListRaw();
		lr.a = List.of(10, 20, 30);
		ListOptionalLong lol = Mappers.map(lr, ListOptionalLong.class);
		assertThat(lol).isNotNull();
		assertThat(lol.a).isNotNull();
		assertThat(lol.a).containsExactly(Optional.of(10L), Optional.of(20L), Optional.of(30L));
	}
	
	@Test
	void testListRawToArrayInt() {
		ListRaw lr = new ListRaw();
		lr.a = List.of(10, 20, 30);
		ArrayInt ai = Mappers.map(lr, ArrayInt.class);
		assertThat(ai).isNotNull();
		assertThat(ai.a).isNotNull();
		assertThat(ai.a).containsExactly(10, 20, 30);
	}
	
	@Test
	void testListRawToArrayOptionalLong() {
		ListRaw lr = new ListRaw();
		lr.a = List.of(10, 20, 30);
		ArrayOptionalLong aol = Mappers.map(lr, ArrayOptionalLong.class);
		assertThat(aol).isNotNull();
		assertThat(aol.a).isNotNull();
		assertThat(aol.a).containsExactly(Optional.of(10L), Optional.of(20L), Optional.of(30L));
	}
	
	@Test
	void testPrimitiveLongToInstant() {
		PrimitiveLong pl = new PrimitiveLong();
		pl.a = System.currentTimeMillis();
		WithInstant i = Mappers.map(pl, WithInstant.class);
		assertThat(i).isNotNull();
		assertThat(i.a).isNotNull();
		assertThat(i.a.toEpochMilli()).isEqualTo(pl.a);
	}
	
	@Test
	void testInstantToLocalDate() {
		WithInstant i = new WithInstant();
		i.a = Instant.now();
		WithLocalDate ld = Mappers.map(i, WithLocalDate.class);
		assertThat(ld).isNotNull();
		assertThat(ld.a).isNotNull();
		assertThat(ld.a).isEqualTo(LocalDate.ofInstant(i.a, ZoneId.systemDefault()));
	}
	
}
