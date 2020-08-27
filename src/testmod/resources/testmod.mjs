const BlockSettings = Java.type("net.minecraft.block.AbstractBlock$Settings");
const Block = Java.type("net.minecraft.block.Block");
const Blocks = Java.type("net.minecraft.block.Blocks");
const Material = Java.type("net.minecraft.block.Material");
const BlockItem = Java.type("net.minecraft.item.BlockItem");
const Item = Java.type("net.minecraft.item.Item");
const ItemSettings = Java.type("net.minecraft.item.Item$Settings");
const ItemGroup = Java.type("net.minecraft.item.ItemGroup");
const ActionResult = Java.type("net.minecraft.util.ActionResult");
const Identifier = Java.type("net.minecraft.util.Identifier");
const Registry = Java.type("net.minecraft.util.registry.Registry");

function register(registry, id, entry) {
  if (typeof id === "string") id = new Identifier("javascript-test", id);
  return Registry.register(registry, id, entry);
}

const blocks = {};
const items = {};

const TestBlock = Java.extend(Block, {
  onUse(_, world, pos) {
    console.log("use test_block");
    world.setBlockState(pos, Blocks.IRON_BLOCK.getDefaultState());
    return ActionResult.SUCCESS;
  }
});

export function onPreLaunch() {
  console.warn("cursed language");
}

export function onInitialize() {
  blocks["test_block"] = register(Registry.BLOCK, "test_block", new TestBlock(BlockSettings.of(Material.METAL)));
  items["test_block"] = register(Registry.ITEM, "test_block", new BlockItem(blocks["test_block"], new ItemSettings().group(ItemGroup.BUILDING_BLOCKS)));
  items["test_block"].appendBlocks(Item.BLOCK_ITEMS, items["test_block"]);
}
