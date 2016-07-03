package edgeville.model.entity.player;

import edgeville.bank.BankTab;
import edgeville.model.entity.Player;
import edgeville.model.entity.player.interfaces.inputdialog.NumberInputDialog;
import edgeville.model.item.Item;
import edgeville.model.item.ItemContainer;
import edgeville.model.item.ItemContainer.Type;
import edgeville.net.message.game.encoders.InterfaceSettings;
import edgeville.net.message.game.encoders.InvokeScript;
import edgeville.util.Varbit;

public class BankNew {
	private Player player;

	public BankTab[] bankTabs = new BankTab[10];

	public BankNew(Player player) {
		this.player = player;
		for (int i = 0; i < bankTabs.length; i++) {
			bankTabs[i] = new BankTab(i, Varbit.BANK_TAB - 1 + i);
		}
	}
	
	private BankTab getBankTabForItem(int itemId) {
		for(int i = 0 ; i < bankTabs.length; i++) {
			BankTab tab = bankTabs[i];
			if (tab.contains(itemId)) {
				return tab;
			}
		}
		return null;
	}

	public void moveItemOnItem(int itemId, int slot, int itemOther, int slotOther) {
		BankTab fromTab = getBankTabForItem(itemOther);
		BankTab targetBankTab = bankTabs[slotOther - 10];
		player.message("Target bank tab : %d", targetBankTab.getId());

		int amount = fromTab.remove(itemOther);
		targetBankTab.add(new Item(itemOther, amount));

		player.getBank().getBankItems().makeDirty();
	}

	public ItemContainer getAllItems() {
		ItemContainer container = new ItemContainer(player.world(), 800, Type.FULL_STACKING);
		for (int i = 1; i < bankTabs.length; i++) {
			BankTab tab = bankTabs[i];
			for (Item item : tab.getItems()) {
				container.add(item);
			}
			player.varps().setVarbit(tab.getVarbit(), tab.getItems().size());
			System.out.println();
		}
		
		// Add the main tab items.
		for (Item item : bankTabs[0].getItems())
			container.add(item);

		return container;
	}

	public void handleClick(int buttonId, int slot, int option) {
		// TODO Auto-generated method stub

	}

	// dont touch below

	public void open() {
		if (player.inWilderness()) {
			player.message("You cannot do this while in the wilderness.");
			return;
		}
		player.write(new InvokeScript(917, -1, -2147483648));
		player.interfaces().sendMain(12, false);
		player.interfaces().send(15, player.interfaces().activeRoot(), (player.interfaces().resizable() ? 56 : 60), false);

		player.write(new InterfaceSettings(12, 12, 0, 799, 1311998));
		player.write(new InterfaceSettings(12, 12, 809, 817, 2));
		player.write(new InterfaceSettings(12, 12, 818, 827, 1048576));
		player.write(new InterfaceSettings(12, 10, 10, 10, 1048578));
		player.write(new InterfaceSettings(12, 10, 11, 19, 1179714));
		player.write(new InterfaceSettings(15, 3, 0, 27, 1181438));
		player.write(new InterfaceSettings(15, 12, 0, 27, 1054));
		player.write(new InterfaceSettings(12, 32, 0, 3, 2));

		player.getBank().getBankItems().makeDirty();
	}

	public int determineAmountToDeposit(int option, int totalAmount, int id) {
		int amount = 1;

		switch (option) {
		case 1:
			amount = 1;
			break;
		case 2:
			amount = 5;
			break;
		case 3:
			amount = 10;
			break;
		case 6:
			amount = totalAmount;
			break;
		}

		return amount;
	}

	public int determineAmountToWithdraw(int option, int totalAmount) {
		int amount = 1;

		switch (option) {
		case 0:
			amount = 1;
			break;
		case 1:
			amount = 5;
			break;
		case 2:
			amount = 10;
			break;
		case 4:
			// TODO:x
			break;
		case 5:
			amount = totalAmount;
			break;
		case 6:
			amount = totalAmount - 1;
			break;
		}

		return amount;
	}

	public void deposit(int buttonId, int slot, int option) {
		slot++;

		// The selected item.
		Item item = player.getInventory().get(slot);

		if (item == null)
			return;

		int id = item.getId();

		// X
		if (option == 5) {
			NumberInputDialog var = new NumberInputDialog(player) {
				@Override
				public void doAction(int value) {
					moveItemsToBank(id, value);
				}
			};
			var.send();
			return;
		}
		int amount = determineAmountToDeposit(option, item.getAmount(), id);
		moveItemsToBank(id, amount);
	}

	private void moveItemsToBank(int id, int amount) {
		int unnotedId = new Item(id).definition(player.world()).unnotedID;
		if (player.getInventory().remove(id, amount).success()) {
			if (unnotedId > 0 && unnotedId < id) {
				bankTabs[0].add(new Item(unnotedId, amount));
				return;
			}
			bankTabs[0].add(new Item(id, amount));
			player.getBank().getBankItems().makeDirty();
		}
	}

	public void shiftItems(int slotFrom, int slotTo) {
		int to = slotTo;
		int tempFrom = slotFrom;

		for (int tempTo = to; tempFrom != tempTo;) {
			if (tempFrom > tempTo) {
				switchItem(tempFrom, tempFrom - 1);
				tempFrom--;
			} else if (tempFrom < tempTo) {
				switchItem(tempFrom, tempFrom + 1);
				tempFrom++;
			}
		}
	}

	public void switchItem(int slotFrom, int slotTo) {
		Item from = bankTabs[0].getItems().get(slotFrom);
		Item to = bankTabs[0].getItems().get(slotTo);

		bankTabs[0].getItems().set(slotFrom, to);
		bankTabs[0].getItems().set(slotTo, from);
	}
}
