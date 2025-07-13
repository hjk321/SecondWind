package gg.hjk.secondwind

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack

internal class SecondWindCommand : BasicCommand {
    override fun execute(stack: CommandSourceStack, args: Array<String>) {
        stack.sender.sendMessage("This is the SecondWind admin command.")
    }
}
