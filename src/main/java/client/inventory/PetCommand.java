package client.inventory;

public record PetCommand(int petId, int skillId, int probability, int increase) {
}
