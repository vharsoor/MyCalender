package dev.sudhanshu.calender.presentation.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sudhanshu.calender.presentation.ui.theme.Typography
import dev.sudhanshu.calender.presentation.view.dao.ShoppingItemDao
import dev.sudhanshu.calender.presentation.view.database.DatabaseProvider
import dev.sudhanshu.calender.presentation.view.model.ShoppingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

class ShoppingListActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = DatabaseProvider.getDatabase(this)
        val shoppingItemDao = db.shoppingItemDao()
        Log.d("ShoppingList", "ShoppingItem DAO")


        setContent{
            MaterialTheme {
                ShoppingListScreen(shoppingItemDao)
            }
        }
    }
}

@Composable
fun ShoppingListScreen(shoppingItemDao: ShoppingItemDao){
    var itemName by remember { mutableStateOf("") }
    val shoppingItems = remember { mutableStateOf<List<ShoppingItem>>(emptyList()) }
    var navigateToMainCalendar by remember { mutableStateOf(false) }
    var context = LocalContext.current
    var initialShoppingList by remember { mutableStateOf(true) }
    var updateShoppingList by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top=50.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Shopping List",
            style = Typography.h1,
            fontSize = 24.sp,
            color = MaterialTheme.colors.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        if(initialShoppingList){
            ItemList(shoppingItemDao, shoppingItems)
        }
        else{
            CheckboxList(shoppingItems.value)
        }


        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = itemName,
            onValueChange = {itemName = it},
            label = {Text("Item Name")},
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {

                addItem(shoppingItemDao, itemName)
                val item = ShoppingItem(name=itemName)
                shoppingItems.value = shoppingItems.value + item
                initialShoppingList = false
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Item")
        }



        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Add padding around the Box
            contentAlignment = Alignment.BottomCenter // Align content to the bottom center
        ) {
            Button(
                onClick = { navigateToMainCalendar = true },
                modifier = Modifier.fillMaxWidth() // Make the button span the full width
            ) {
                Text("Calendar View")
            }
        }


    }
    if(navigateToMainCalendar){
        var intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
        navigateToMainCalendar = false
    }

}

@Composable
fun ItemList(shoppingItemDao: ShoppingItemDao, shoppingItems: MutableState<List<ShoppingItem>>){

    LaunchedEffect(Unit) {
        val fetchedItems = shoppingItemDao.getAllItems() // Ensure this is a suspend function
        shoppingItems.value = fetchedItems
    }

    CheckboxList(shoppingItems.value)
}


@Composable
fun CheckboxList(shoppingItems: List<ShoppingItem>) {
    // Manage the state for checked items
    var checkedStates by remember { mutableStateOf(shoppingItems.associate { it.id to false }) }

    LazyColumn(
        modifier = Modifier
            .height(500.dp)
            .padding(16.dp)
    ) {
        items(shoppingItems) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = item.name, modifier = Modifier.weight(1f)) // Item name
                Checkbox(
                    checked = checkedStates[item.id] ?: false,
                    onCheckedChange = { isChecked ->
                        // Update the state for this item
                        checkedStates = checkedStates.toMutableMap().apply { put(item.id, isChecked) }
                    }
                )
            }
        }
    }
}
fun fetchItems(shoppingItemDao: ShoppingItemDao, callback: (List<ShoppingItem>) -> Unit){
    CoroutineScope(Dispatchers.IO).launch {
        val items = shoppingItemDao.getAllItems()
        CoroutineScope(Dispatchers.Main).launch {
            callback(items)
        }
    }
}

fun addItem(shoppingItemDao: ShoppingItemDao, name:String){
    CoroutineScope(Dispatchers.IO).launch{
        shoppingItemDao.insertItem(ShoppingItem(name=name))
    }
}