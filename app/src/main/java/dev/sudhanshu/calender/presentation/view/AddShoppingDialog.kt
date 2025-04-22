/**
 * AddShoppingDialog.kt (In Progress)
 *
 * This file defines a simple Compose-based dialog that allows the user to add a new shopping item.
 *
 * Usage:
 * - Triggered when the user taps a button (likely a "+" or "Add Item" button).
 * - Pops up a dialog with a text field to enter the item name.
 * - Includes "Cancel" and "Add" buttons to dismiss or submit the input.
 *
 * This dialog is a clean and lightweight way to collect user input for a shopping list.
 */



package dev.sudhanshu.calender.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AddItemDialog(
    onAddItem: (String) -> Unit,
    onDismiss: () -> Unit
){
    var itemName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss){
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ){
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ){
                Text(
                    text = "Add Shopping Item",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = itemName,
                    onValueChange = {itemName = it},
                    label = {Text("Item Name")},
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Cancel
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ){
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if(itemName.isNotBlank()){
                                onAddItem(itemName)
                            }
                        }
                    ){
                        Text("Add")
                    }
                }
            }
        }
    }
}