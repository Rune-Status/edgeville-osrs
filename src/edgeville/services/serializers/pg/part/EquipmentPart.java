package edgeville.services.serializers.pg.part;

import com.google.gson.*;

import edgeville.model.entity.Player;
import edgeville.model.item.Item;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Bart on 8/10/2015.
 */
public class EquipmentPart implements PgJsonPart {

	private JsonParser parser = new JsonParser();
	private Gson gson = new Gson();

	@Override
	public void decode(Player player, ResultSet resultSet) throws SQLException {
		JsonObject equipment = parser.parse(resultSet.getString("equipment")).getAsJsonObject();
		JsonArray itemarray = equipment.getAsJsonArray("items");
		for (JsonElement item_ : itemarray) {
			JsonObject item = item_.getAsJsonObject();

			// TODO properties
			Item itemobj = new Item(item.get("id").getAsInt(), item.get("amount").getAsInt());
			player.getEquipment().set(item.get("slot").getAsInt(), itemobj);
		}
	}

	@Override
	public void encode(Player player, PreparedStatement characterUpdateStatement) throws SQLException {
		JsonArray itemarray = new JsonArray();
		for (int i = 0; i < 14; i++) {
			Item item = player.getEquipment().get(i);
			if (item != null) {
				JsonObject obj = new JsonObject();
				obj.add("slot", new JsonPrimitive(i));
				obj.add("id", new JsonPrimitive(item.id()));
				obj.add("amount", new JsonPrimitive(item.amount()));
				itemarray.add(obj);
			}
		}

		JsonObject itemobj = new JsonObject();
		itemobj.add("items", itemarray);

		characterUpdateStatement.setString(5, gson.toJson(itemobj));
	}

}
