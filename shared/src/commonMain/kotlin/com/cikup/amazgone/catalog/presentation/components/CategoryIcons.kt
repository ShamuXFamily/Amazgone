package com.cikup.amazgone.catalog.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tablet
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon for a catalog category slug (DummyJSON + CheapShark), with a generic fallback. */
fun categoryIcon(slug: String): ImageVector = when {
    slug == "video-games" -> Icons.Outlined.SportsEsports
    slug == "beauty" -> Icons.Outlined.Face
    slug == "fragrances" || slug == "skin-care" -> Icons.Outlined.Spa
    slug == "furniture" -> Icons.Outlined.Chair
    slug == "groceries" -> Icons.Outlined.LocalGroceryStore
    slug == "home-decoration" -> Icons.Outlined.Home
    slug == "kitchen-accessories" -> Icons.Outlined.Kitchen
    slug == "laptops" -> Icons.Outlined.Laptop
    slug == "smartphones" -> Icons.Outlined.Smartphone
    slug == "tablets" -> Icons.Outlined.Tablet
    slug == "mobile-accessories" -> Icons.Outlined.Headphones
    slug == "motorcycle" -> Icons.Outlined.TwoWheeler
    slug == "vehicle" -> Icons.Outlined.DirectionsCar
    slug == "sports-accessories" -> Icons.Outlined.SportsBasketball
    slug == "sunglasses" -> Icons.Outlined.WbSunny
    slug.endsWith("watches") -> Icons.Outlined.Watch
    slug.endsWith("shoes") -> Icons.Outlined.DirectionsRun
    slug.endsWith("jewellery") -> Icons.Outlined.Diamond
    slug.endsWith("bags") -> Icons.Outlined.ShoppingBag
    slug.endsWith("shirts") || slug.endsWith("dresses") || slug == "tops" -> Icons.Outlined.Checkroom
    else -> Icons.Outlined.Category
}
