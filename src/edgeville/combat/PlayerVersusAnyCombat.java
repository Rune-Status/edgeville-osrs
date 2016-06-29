package edgeville.combat;

import edgeville.model.AttributeKey;
import edgeville.model.Entity;
import edgeville.model.Tile;
import edgeville.model.entity.Player;
import edgeville.model.entity.player.EquipSlot;
import edgeville.model.entity.player.Skulls;
import edgeville.model.entity.player.WeaponType;
import edgeville.model.item.Item;
import edgeville.script.TimerKey;
import edgeville.util.AccuracyFormula;
import edgeville.util.CombatFormula;
import edgeville.util.CombatStyle;
import edgeville.util.EquipmentInfo;
import edgeville.util.Varp;

public class PlayerVersusAnyCombat extends Combat {

	private Player player;
	private Entity target;

	public PlayerVersusAnyCombat(Entity entity, Entity target) {
		super(entity, target);
		// TODO Auto-generated constructor stub
		player = (Player) entity;
		this.target = target;
	}

	@Override
	public void cycle() {
		// Get weapon data.
		Item weapon = ((Player) getEntity()).getEquipment().get(EquipSlot.WEAPON);
		int weaponId = weapon == null ? -1 : weapon.id();
		int weaponType = getEntity().world().equipmentInfo().weaponType(weaponId);
		Item ammo = ((Player) getEntity()).getEquipment().get(EquipSlot.AMMO);
		int ammoId = ammo == null ? -1 : ammo.id();
		String ammoName = ammo == null ? "" : ammo.definition(getEntity().world()).name;

		// Check if players are in wilderness.
		if (target instanceof Player) {
			if (!player.inWilderness() || !((Player) target).inWilderness()) {
				player.message("You or your target are not in wild.");
				return;
			}
		}

		// Combat type?
		if (weaponType == WeaponType.BOW || weaponType == WeaponType.CROSSBOW || weaponType == WeaponType.THROWN) {
			getEntity().message("ranging...");
			handleRangeCombat(weaponId, ammoName, weaponType);
		} else {
			getEntity().message("meleeeing...");
			handleMeleeCombat(weaponId);
		}

		getTarget().putattrib(AttributeKey.LAST_DAMAGER, getEntity());
		getTarget().putattrib(AttributeKey.LAST_DAMAGE, System.currentTimeMillis());

		if (target instanceof Player) {
			player.setSkullHeadIcon(Skulls.WHITE_SKUL.getSkullId());
			player.timers().extendOrRegister(TimerKey.SKULL, 2000); // 20 minutes
		}
	}

	public void handleMeleeCombat(int weaponId) {
		Tile currentTile = getEntity().getTile();

		// Move closer if out of range.
		if (!player.touches(getTarget(), currentTile) && !getEntity().frozen() && !getEntity().stunned()) {
			currentTile = moveCloser();
			return;
		}

		// Check timer.
		if (!getEntity().timers().has(TimerKey.COMBAT_ATTACK)) {

			if (player.isSpecialAttackEnabled()) {
				doMeleeSpecial(player, weaponId);
				return;
			}

			boolean success = AccuracyFormula.doesHit(((Player) getEntity()), getTarget(), CombatStyle.MELEE);

			int max = CombatFormula.maximumMeleeHit(((Player) getEntity()));
			int hit = getEntity().world().random(max);

			getTarget().hit(getEntity(), success ? hit : 0);

			getEntity().animate(EquipmentInfo.attackAnimationFor(((Player) getEntity())));
			getEntity().timers().register(TimerKey.COMBAT_ATTACK, getEntity().world().equipmentInfo().weaponSpeed(weaponId));
		}
	}

	public void doMeleeSpecial(Player player, int weaponId) {
		switch (weaponId) {

		// dds
		case 5698:
			player.timers().register(TimerKey.COMBAT_ATTACK, 5);

			if (!drainSpecialEnergy(player, 25)) {
				return;
			}

			player.animate(1062);
			player.graphic(252, 92, 0);

			double max = CombatFormula.maximumMeleeHit(player) * 1.15;
			int hit = player.world().random().nextInt((int) Math.round(max));
			int hit2 = player.world().random().nextInt((int) Math.round(max));

			target.hit(player, hit);
			target.hit(player, hit2);
			break;
		}
	}

	private void handleRangeCombat(int weaponId, String ammoName, int weaponType) {
		Tile currentTile = player.getTile();

		// Are we in range?
		if (currentTile.distance(target.getTile()) > 7 && !player.frozen() && !player.stunned()) {
			currentTile = moveCloser();
		}

		// Can we shoot?
		if (player.timers().has(TimerKey.COMBAT_ATTACK)) {
			return;
		}

		// Do we have ammo?
		if (ammoName.equals("")) {
			player.message("There's no ammo left in your quiver.");
			// container.stop();
			return;
		}

		// Check if ammo is of right type
		if (weaponType == WeaponType.CROSSBOW && !ammoName.contains(" bolts")) {
			player.message("You can't use that ammo with your crossbow.");
			// container.stop();
			return;
		}
		if (weaponType == WeaponType.BOW && !ammoName.contains(" arrow")) {
			player.message("You can't use that ammo with your bow.");
			// container.stop();
			return;
		}

		// Remove the ammo
		Item ammo = player.getEquipment().get(EquipSlot.AMMO);
		player.getEquipment().set(EquipSlot.AMMO, new Item(ammo.id(), ammo.amount() - 1));

		player.animate(EquipmentInfo.attackAnimationFor(player));
		int distance = player.getTile().distance(target.getTile());
		int cyclesPerTile = 5;
		int baseDelay = 32;
		int startHeight = 35;
		int endHeight = 36;
		int curve = 15;
		int graphic = 228;

		if (weaponType == WeaponType.CROSSBOW) {
			cyclesPerTile = 3;
			baseDelay = 40;
			startHeight = 40;
			endHeight = 40;
			curve = 2;
			graphic = 27;
		}

		if (player.varps().getVarp(Varp.SPECIAL_ENABLED) == 0 && doRangeSpecial()) {
			return;
		}

		player.world().spawnProjectile(player.getTile(), target, graphic, startHeight, endHeight, baseDelay, cyclesPerTile * distance, curve, 105);

		long delay = Math.round(Math.floor(baseDelay / 30.0) + (distance * (cyclesPerTile * 0.020) / 0.6));
		boolean success = AccuracyFormula.doesHit(player, target, CombatStyle.RANGE);

		int maxHit = CombatFormula.maximumRangedHit(player);
		int hit = player.world().random(maxHit);

		// target.hit(player, success ? hit : 0,
		// delay).combatStyle(CombatStyle.RANGE);

		target.hit(player, success ? hit : 0, (int) delay).combatStyle(CombatStyle.RANGE);

		// Timer is downtime.
		player.timers().register(TimerKey.COMBAT_ATTACK, player.world().equipmentInfo().weaponSpeed(weaponId));

		// After every attack, reset special.
		player.varps().setVarp(Varp.SPECIAL_ENABLED, 0);
	}

	private boolean doRangeSpecial() {
		int weaponId = ((Player) getEntity()).getEquipment().get(EquipSlot.WEAPON).id();

		switch (weaponId) {
		
		// Magic short bow
		case 861:

			break;
		}

		return false;// TODO
	}

	public static void handleGraniteMaul(Player player, Entity target) {
		if (player.getSpecialEnergyAmount() < 50 * 10) {
			player.message("You do not have enough special energy.");
			return;
		}

		if (!player.touches(target, player.getTile())) {
			return;
		}

		player.setSpecialEnergyAmount(player.getSpecialEnergyAmount() - (50 * 10));

		player.animate(1667);
		player.graphic(340, 92, 0);

		double max = CombatFormula.maximumMeleeHit(player);
		int hit = player.world().random().nextInt((int) Math.round(max));
		target.hit(player, hit);
		player.timers().register(TimerKey.COMBAT_ATTACK, target.world().equipmentInfo().weaponSpeed(4153));
	}

	private boolean drainSpecialEnergy(Player player, int amount) {
		player.toggleSpecialAttack();
		if (player.getSpecialEnergyAmount() < amount * 10) {
			player.message("You do not have enough special energy.");
			return false;
		}
		player.setSpecialEnergyAmount(player.getSpecialEnergyAmount() - (amount * 10));
		return true;
	}
}
