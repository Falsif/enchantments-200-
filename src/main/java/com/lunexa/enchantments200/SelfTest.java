package com.lunexa.enchantments200;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Runs on a real dedicated server with -De200.selftest=true, writes selftest-report.txt and stops the server. */
public final class SelfTest {
	private final List<String> lines = new ArrayList<>();
	private int pass;
	private int fail;

	private void check(String name, boolean ok, Object detail) {
		if (ok) pass++; else fail++;
		lines.add((ok ? "[PASS] " : "[FAIL] ") + name + " -> " + detail);
	}

	public static void run(MinecraftServer server) {
		SelfTest t = new SelfTest();
		try {
			t.runAll(server);
		} catch (Throwable e) {
			t.fail++;
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			t.lines.add("[FAIL] EXCEPTION " + sw);
		}
		t.lines.add("");
		t.lines.add((t.fail == 0 ? "RESULT: PASSED " : "RESULT: FAILED ") + t.pass + " passed, " + t.fail + " failed");
		try {
			Files.write(Path.of("selftest-report.txt"), t.lines, StandardCharsets.UTF_8);
		} catch (Exception e) {
			Enchantments200.LOGGER.error("could not write report", e);
		}
		for (String l : t.lines) Enchantments200.LOGGER.info("[SelfTest] {}", l);
		server.halt(false);
	}

	private static ItemStack enchanted(Item item, Holder<Enchantment> h, int level) {
		ItemStack s = new ItemStack(item);
		s.enchant(h, level);
		return s;
	}

	private void runAll(MinecraftServer server) throws Exception {
		ServerLevel level = server.overworld();
		Registry<Enchantment> reg = server.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		int max = E200Config.get().clampedMaxLevel();

		// ---- mixins actually applied ----
		// Touch every mixin target (names must work in dev and in the release jar).
		check("mixin targets load", AnvilMenu.class != null && CombatRules.class != null && EnchantmentHelper.class != null, "ok");
		try {
			MixinEnvironment.getCurrentEnvironment().audit();
			check("mixin audit", true, "ok");
		} catch (Throwable e) {
			check("mixin audit", false, e.toString());
		}

		// ---- every enchantment: max 200, book, name, save/load ----
		RegistryOps<Tag> ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
		String expectedNumeral = E200Text.levelText(max);
		int count = 0;
		for (Holder.Reference<Enchantment> h : reg.holders().toList()) {
			count++;
			String id = h.key().location().toString();
			int m = h.value().getMaxLevel();
			ItemStack book = E200Books.book(h, m);
			Tag tag = ItemStack.CODEC.encodeStart(ops, book).getOrThrow();
			ItemStack back = ItemStack.CODEC.parse(ops, tag).getOrThrow();
			String fullName = Enchantment.getFullname(h, m).getString();
			String bookName = book.getHoverName().getString();
			boolean ok = m == max
					&& E200Books.storedLevel(book, h) == max
					&& E200Books.storedLevel(back, h) == max
					&& fullName.endsWith(" " + expectedNumeral)
					&& bookName.contains(expectedNumeral);
			check("enchantment " + id, ok, "max=" + m + " name='" + fullName + "' book='" + bookName + "' saved=" + E200Books.storedLevel(back, h));
		}
		check("number of enchantments >= 39", count >= 39, count);

		Holder<Enchantment> sharp = reg.getHolderOrThrow(Enchantments.SHARPNESS);
		Holder<Enchantment> smite = reg.getHolderOrThrow(Enchantments.SMITE);
		Holder<Enchantment> prot = reg.getHolderOrThrow(Enchantments.PROTECTION);
		Holder<Enchantment> feather = reg.getHolderOrThrow(Enchantments.FEATHER_FALLING);
		Holder<Enchantment> unbreaking = reg.getHolderOrThrow(Enchantments.UNBREAKING);
		Holder<Enchantment> efficiency = reg.getHolderOrThrow(Enchantments.EFFICIENCY);
		Holder<Enchantment> fortune = reg.getHolderOrThrow(Enchantments.FORTUNE);
		Holder<Enchantment> looting = reg.getHolderOrThrow(Enchantments.LOOTING);
		Holder<Enchantment> knockback = reg.getHolderOrThrow(Enchantments.KNOCKBACK);
		Holder<Enchantment> power = reg.getHolderOrThrow(Enchantments.POWER);
		Holder<Enchantment> riptide = reg.getHolderOrThrow(Enchantments.RIPTIDE);
		Holder<Enchantment> multishot = reg.getHolderOrThrow(Enchantments.MULTISHOT);
		Holder<Enchantment> frost = reg.getHolderOrThrow(Enchantments.FROST_WALKER);

		Zombie target = EntityType.ZOMBIE.create(level);
		Zombie attacker = EntityType.ZOMBIE.create(level);
		DamageSource melee = level.damageSources().mobAttack(attacker);

		// ---- Sharpness / Smite ----
		float d5 = EnchantmentHelper.modifyDamage(level, enchanted(Items.DIAMOND_SWORD, sharp, 5), target, melee, 1.0F);
		float d200 = EnchantmentHelper.modifyDamage(level, enchanted(Items.DIAMOND_SWORD, sharp, 200), target, melee, 1.0F);
		check("Sharpness CC extra damage", d200 > 90 && d200 > d5 * 10, "V=" + d5 + " CC=" + d200);
		float s200 = EnchantmentHelper.modifyDamage(level, enchanted(Items.DIAMOND_SWORD, smite, 200), target, melee, 1.0F);
		check("Smite CC vs zombie", s200 > 400, s200);

		// ---- Protection ----
		Zombie armored = EntityType.ZOMBIE.create(level);
		armored.setItemSlot(EquipmentSlot.CHEST, enchanted(Items.DIAMOND_CHESTPLATE, prot, 200));
		float p = EnchantmentHelper.getDamageProtection(level, armored, melee);
		float after = CombatRules.getDamageAfterMagicAbsorb(100.0F, p);
		check("Protection CC points", p >= 150, p);
		check("Protection CC: 100 damage becomes", after > 0 && after < 10, after);
		check("Protection IV still vanilla (16 pts = 64%)", Math.abs(CombatRules.getDamageAfterMagicAbsorb(100.0F, 16.0F) - 36.0F) < 0.01F, CombatRules.getDamageAfterMagicAbsorb(100.0F, 16.0F));
		armored.setItemSlot(EquipmentSlot.FEET, enchanted(Items.DIAMOND_BOOTS, feather, 200));
		float pf = EnchantmentHelper.getDamageProtection(level, armored, level.damageSources().fall());
		check("Feather Falling CC + Protection CC vs fall", pf >= 500, pf);

		// ---- Unbreaking ----
		int dmg = EnchantmentHelper.processDurabilityChange(level, enchanted(Items.DIAMOND_PICKAXE, unbreaking, 200), 10000);
		check("Unbreaking CC: 10000 durability hits become", dmg < 300, dmg);

		// ---- Efficiency ----
		double[] eff = {0};
		EnchantmentHelper.forEachModifier(enchanted(Items.DIAMOND_PICKAXE, efficiency, 200), EquipmentSlot.MAINHAND, (attr, mod) -> {
			if (attr.equals(Attributes.MINING_EFFICIENCY)) eff[0] += mod.amount();
		});
		check("Efficiency CC mining bonus", eff[0] > 10000, eff[0]);

		// ---- Fortune (real loot table) ----
		ItemStack fortunePick = enchanted(Items.DIAMOND_PICKAXE, fortune, 200);
		int diamonds = 0;
		for (int i = 0; i < 30; i++) {
			for (ItemStack drop : Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, BlockPos.ZERO, null, null, fortunePick)) {
				diamonds += drop.getCount();
			}
		}
		check("Fortune CC: diamonds from 30 ores", diamonds >= 30 * 20, diamonds);

		// ---- Looting ----
		check("Looting CC level read by loot tables", EnchantmentHelper.getItemEnchantmentLevel(looting, enchanted(Items.DIAMOND_SWORD, looting, 200)) == 200, "ok");

		// ---- Knockback (capped) ----
		float kb = EnchantmentHelper.modifyKnockback(level, enchanted(Items.DIAMOND_SWORD, knockback, 200), target, melee, 0.0F);
		check("Knockback CC (safety cap 30)", kb >= 10 && kb <= 40, kb);

		// ---- Power ----
		Arrow arrow = EntityType.ARROW.create(level);
		ItemStack bow = enchanted(Items.BOW, power, 200);
		float pw = EnchantmentHelper.modifyDamage(level, bow, target, level.damageSources().arrow(arrow, attacker), 2.0F);
		check("Power CC arrow damage", pw > 50, pw);

		// ---- Safety caps ----
		float spin = EnchantmentHelper.getTridentSpinAttackStrength(enchanted(Items.TRIDENT, riptide, 200), attacker);
		check("Riptide CC capped (no flying out of the world)", spin > 2 && spin < 20, spin);
		int shots = EnchantmentHelper.processProjectileCount(level, enchanted(Items.CROSSBOW, multishot, 200), attacker, 1);
		check("Multishot CC capped", shots > 3 && shots <= 40, shots);
		check("Frost Walker effect level capped", E200Levels.effectiveLevel(frost, 200) == 16, E200Levels.effectiveLevel(frost, 200));
		check("Sharpness not capped", E200Levels.effectiveLevel(sharp, 200) == 200, "ok");

		// ---- Anvil ----
		FakePlayer fp = FakePlayer.get(level);
		check("anvil 100 + 100 = 200", anvil(fp, E200Books.book(sharp, 100), E200Books.book(sharp, 100), sharp, 200), lastAnvil);
		check("anvil 64 + 64 = 128", anvil(fp, E200Books.book(sharp, 64), E200Books.book(sharp, 64), sharp, 128), lastAnvil);
		check("anvil 150 + 150 = 300", anvil(fp, E200Books.book(sharp, 150), E200Books.book(sharp, 150), sharp, 300), lastAnvil);
		check("anvil 500M + 500M = 1 000 000 000", anvil(fp, E200Books.book(sharp, 500_000_000), E200Books.book(sharp, 500_000_000), sharp, 1_000_000_000), lastAnvil);
		check("anvil 600M + 600M = max (no overflow)", anvil(fp, E200Books.book(sharp, 600_000_000), E200Books.book(sharp, 600_000_000), sharp, max), lastAnvil);
		check("anvil sword Sharpness 1 000 000 000 + book V", anvil(fp, enchanted(Items.NETHERITE_SWORD, sharp, 1_000_000_000), E200Books.book(sharp, 5), sharp, max), lastAnvil);
		{
			ItemStack big = E200Books.book(sharp, 1_000_000_000);
			EnchantmentHelper.updateEnchantments(big, m -> {
				m.set(looting, 1_000_000_000);
				m.set(unbreaking, 1_000_000_000);
				m.set(knockback, 1_000_000_000);
			});
			boolean ok = anvil(fp, new ItemStack(Items.DIAMOND_SWORD), big, sharp, 1_000_000_000);
			ItemStack out = lastOut;
			int lo = EnchantmentHelper.getEnchantmentsForCrafting(out).getLevel(looting);
			int un = EnchantmentHelper.getEnchantmentsForCrafting(out).getLevel(unbreaking);
			check("anvil book with 4 billion-level enchantments onto sword", ok && lo == 1_000_000_000 && un == 1_000_000_000, lastAnvil + " looting=" + lo + " unbreaking=" + un);
		}
		check("anvil: incompatible Smite not added to Sharpness sword", anvilEmpty(fp, enchanted(Items.DIAMOND_SWORD, sharp, 1_000_000_000), E200Books.book(smite, 1_000_000_000)), lastAnvil);
		check("anvil sword V + book CL = CLV", anvil(fp, enchanted(Items.DIAMOND_SWORD, sharp, 5), E200Books.book(sharp, 150), sharp, 155), lastAnvil);
		check("anvil book CC onto plain sword", anvil(fp, new ItemStack(Items.DIAMOND_SWORD), E200Books.book(sharp, 200), sharp, 200), lastAnvil);
		check("anvil book Protection CC onto chestplate", anvil(fp, new ItemStack(Items.NETHERITE_CHESTPLATE), E200Books.book(prot, 200), prot, 200), lastAnvil);

		// ---- Natural sources keep vanilla levels ----
		check("natural max of Sharpness = 5", E200Levels.naturalMax(sharp.value()) == 5, E200Levels.naturalMax(sharp.value()));
		Optional<HolderSet.Named<Enchantment>> table = reg.getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
		int highest = 0;
		RandomSource rnd = RandomSource.create(42);
		for (int i = 0; i < 200; i++) {
			Stream<Holder<Enchantment>> pool = table.map(HolderSet::stream).orElse(Stream.empty());
			for (EnchantmentInstance inst : EnchantmentHelper.selectEnchantment(rnd, new ItemStack(Items.DIAMOND_SWORD), 30, pool)) {
				highest = Math.max(highest, inst.level);
			}
		}
		check("enchanting table still gives vanilla levels", highest > 0 && highest <= 5, "highest=" + highest);

		// ---- 1 000 000 000 ----
		int B = 1_000_000_000;
		check("max level is 1 000 000 000", max == B, max);
		float dB = EnchantmentHelper.modifyDamage(level, enchanted(Items.NETHERITE_SWORD, sharp, B), target, melee, 1.0F);
		check("Sharpness 1 000 000 000 extra damage", dB > 4.0E8F, dB);
		Zombie armoredB = EntityType.ZOMBIE.create(level);
		armoredB.setItemSlot(EquipmentSlot.CHEST, enchanted(Items.DIAMOND_CHESTPLATE, prot, B));
		float pB = EnchantmentHelper.getDamageProtection(level, armoredB, melee);
		float afterB = CombatRules.getDamageAfterMagicAbsorb(100.0F, pB);
		check("Protection 1 000 000 000: 100 damage becomes", afterB > 0.5F && afterB < 2.0F, afterB);
		{
			double[] effB = {0};
			EnchantmentHelper.forEachModifier(enchanted(Items.DIAMOND_PICKAXE, efficiency, B), EquipmentSlot.MAINHAND, (attr, mod) -> {
				if (attr.equals(Attributes.MINING_EFFICIENCY)) effB[0] += mod.amount();
			});
			check("Efficiency 1 000 000 000 is positive (no overflow)", effB[0] > 1.0E17, effB[0]);
			// Every attribute bonus of every enchantment keeps its sign at 1 000 000 000.
			for (Holder.Reference<Enchantment> h : reg.holders().toList()) {
				for (var ae : h.value().getEffects(net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES)) {
					float a1 = ae.amount().calculate(1);
					float aB = ae.amount().calculate(B);
					boolean ok = Float.isFinite(aB) && (a1 == 0 || Math.signum(a1) == Math.signum(aB));
					check("attribute bonus sign at 1 000 000 000: " + h.key().location().getPath() + " " + ae.attribute().getRegisteredName(), ok, "lvl1=" + a1 + " lvlB=" + aB);
				}
			}
		}
		check("Unbreaking 1 000 000 000: 10000 hits become", EnchantmentHelper.processDurabilityChange(level, enchanted(Items.DIAMOND_PICKAXE, unbreaking, B), 10000) == 0, "0");
		ItemStack pickB = enchanted(Items.DIAMOND_PICKAXE, fortune, B);
		long tF = System.nanoTime();
		int dropsB = 0;
		for (ItemStack drop : Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, BlockPos.ZERO, null, null, pickB)) dropsB += drop.getCount();
		check("Fortune 1 000 000 000 (effect cap 1000) is fast and safe", dropsB >= 1 && dropsB <= 1001 && (System.nanoTime() - tF) < 2_000_000_000L, dropsB + " diamonds");
		{
			ItemStack sw = enchanted(Items.NETHERITE_SWORD, sharp, B);
			Tag t2 = ItemStack.CODEC.encodeStart(ops, sw).getOrThrow();
			ItemStack back2 = ItemStack.CODEC.parse(ops, t2).getOrThrow();
			check("sword with Sharpness 1 000 000 000 saves and loads", EnchantmentHelper.getItemEnchantmentLevel(sharp, back2) == B, t2);
			String n = Enchantment.getFullname(sharp, B).getString();
			check("name shows 1 000 000 000", n.endsWith("1 000 000 000"), n);
		}

		// ---- Billion Zombie spawn egg ----
		check("spawn egg registered", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Enchantments200.BILLION_ZOMBIE_EGG).toString().equals("enchantments200:billion_zombie_spawn_egg"),
				net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Enchantments200.BILLION_ZOMBIE_EGG));
		BlockPos zp = level.getSharedSpawnPos();
		Zombie bz = BillionZombie.spawn(level, zp);
		check("billion zombie spawned", bz != null && bz.isAlive() && level.getEntity(bz.getUUID()) != null, bz);
		ItemStack bzSword = bz.getMainHandItem();
		check("billion zombie: netherite sword Sharpness 1 000 000 000", bzSword.is(Items.NETHERITE_SWORD) && EnchantmentHelper.getItemEnchantmentLevel(sharp, bzSword) == B,
				bzSword + " lvl=" + EnchantmentHelper.getItemEnchantmentLevel(sharp, bzSword));
		check("billion zombie: full netherite armor", bz.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET) && bz.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
				&& bz.getItemBySlot(EquipmentSlot.LEGS).is(Items.NETHERITE_LEGGINGS) && bz.getItemBySlot(EquipmentSlot.FEET).is(Items.NETHERITE_BOOTS), "ok");
		check("billion zombie: does not despawn", bz.isPersistenceRequired(), "ok");
		Zombie victim = EntityType.ZOMBIE.create(level);
		victim.moveTo(zp.getX() + 1.5, zp.getY(), zp.getZ() + 0.5);
		victim.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
		level.addFreshEntity(victim);
		boolean hit = bz.doHurtTarget(victim);
		check("billion zombie one-shots an armored mob", hit && victim.isDeadOrDying(), "hit=" + hit + " health=" + victim.getHealth());
		bz.discard();
		victim.discard();

		// ---- vanilla creative tabs do not list a billion books ----
		long tTabs = System.nanoTime();
		net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(server.getWorldData().enabledFeatures(), true, server.registryAccess());
		long tabsMs = (System.nanoTime() - tTabs) / 1_000_000;
		int searchSize = net.minecraft.world.item.CreativeModeTabs.searchTab().getSearchTabDisplayItems().size();
		check("creative tabs build fast", tabsMs < 20000 && searchSize > 1000 && searchSize < 100000, tabsMs + " ms, search tab items=" + searchSize);

		// ---- Roman numerals ----
		check("roman numerals", E200Text.levelText(1).equals("I") && E200Text.levelText(4).equals("IV")
				&& E200Text.levelText(14).equals("XIV") && E200Text.levelText(99).equals("XCIX")
				&& E200Text.levelText(200).equals("CC") && E200Text.levelText(399).equals("CCCXCIX")
				&& E200Text.levelText(400).equals("400") && E200Text.levelText(1_000_000_000).equals("1 000 000 000"), E200Text.levelText(200));

		// ---- Commands ----
		CommandSourceStack src = server.createCommandSourceStack().withSuppressedOutput();
		var disp = server.getCommands().getDispatcher();
		check("/enchant200 help", disp.execute("enchant200 help", src) > 0, "ok");
		check("/enchant200 list", disp.execute("enchant200 list", src) == count, "ok");
		check("/enchant200 reload", disp.execute("enchant200 reload", src) > 0, "ok");
		check("/enchant200 give parses 'sharpness 200'", disp.parse("enchant200 give Steve sharpness 200", src).getExceptions().isEmpty(), "ok");
		check("/enchant200 max parses", disp.parse("enchant200 max protection", src).getExceptions().isEmpty(), "ok");

		// ---- Creative tab ----
		check("creative tab registered", net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(Enchantments200.TAB_KEY), "ok");
	}

	private String lastAnvil = "";
	private ItemStack lastOut = ItemStack.EMPTY;

	private boolean anvilEmpty(FakePlayer fp, ItemStack left, ItemStack right) {
		AnvilMenu menu = new AnvilMenu(0, fp.getInventory());
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		lastAnvil = "result=" + menu.getSlot(2).getItem();
		return menu.getSlot(2).getItem().isEmpty();
	}

	private boolean anvil(FakePlayer fp, ItemStack left, ItemStack right, Holder<Enchantment> h, int expected) {
		AnvilMenu menu = new AnvilMenu(0, fp.getInventory());
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		menu.createResult();
		ItemStack out = menu.getSlot(2).getItem();
		int lvl = EnchantmentHelper.getEnchantmentsForCrafting(out).getLevel(h);
		int cost = menu.getCost();
		lastOut = out;
		lastAnvil = "result=" + (out.isEmpty() ? "EMPTY" : out.getItem() + " lvl " + lvl) + " cost=" + cost;
		return !out.isEmpty() && lvl == expected && cost > 0 && cost <= E200Config.get().clampedAnvilMaxCost();
	}
}
