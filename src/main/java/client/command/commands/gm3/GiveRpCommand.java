package client.command.commands.gm3;

import client.MapleCharacter;
import client.MapleClient;
import client.command.Command;

public class GiveRpCommand extends Command {
    {
        setDescription("");
    }

    private static void giveRpError(MapleCharacter player, String targetName) {
        player.message("Player '" + targetName + "' could not be found.");
    }

    private static void giveRp(MapleCharacter player, String targetName, int points, MapleCharacter victim) {
        victim.setRewardPoints(victim.getRewardPoints() + points);
        player.message("RP given. Player " + targetName + " now has " + victim.getRewardPoints()
                + " reward points.");
    }

    @Override
    public void execute(MapleClient client, String[] params) {
        MapleCharacter player = client.getPlayer();
        if (params.length < 2) {
            player.yellowMessage("Syntax: !giverp <playername> <gainrewardpoint>");
            return;
        }

        String targetName = params[0];
        int points = Integer.parseInt(params[1]);

        client.getWorldServer()
                .getPlayerStorage()
                .getCharacterByName(targetName)
                .ifPresentOrElse(t -> giveRp(player, targetName, points, t), () -> giveRpError(player, targetName));
    }
}
