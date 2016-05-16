package anticheat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import anticheat.events.PlayerLeaveEvent;
import anticheat.util.AsyncDelayedTask;
import anticheat.util.DB;
import anticheat.util.EventUtil;

public class AntiHighJump extends AntiCheatBase {
	private List<String> reported = null;
	
	public AntiHighJump() {
		super("High Jump");
		reported = new ArrayList<String>();
		EventUtil.register(this);
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location to = event.getTo();
		Location from = event.getFrom();
		String y = "" + (to.getY() - from.getY());
		if(y.startsWith("1.02000")) {
			if(!reported.contains(player.getName())) {
				reported.add(player.getName());
				final UUID uuid = player.getUniqueId();
				new AsyncDelayedTask(new Runnable() {
					@Override
					public void run() {
						DB.NETWORK_HIGH_JUMP_TEST.insert("'" + uuid.toString() + "'");
					}
				});
			}
		}
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerLeaveEvent event) {
		reported.remove(event.getPlayer().getName());
	}
}