package com.vardash.mafimushkil.models

import com.vardash.mafimushkil.R

data class ServiceCategoryOption(
    val labelResId: Int,
    val category: Category
)

val serviceCategoryOptions = listOf(
    ServiceCategoryOption(R.string.cat_cleaning, Category("cat_cleaning", "Cleaning", "cleaning", true)),
    ServiceCategoryOption(R.string.cat_electrician, Category("cat_electrician", "Electrician", "electrician", true)),
    ServiceCategoryOption(R.string.cat_plumber, Category("cat_plumber", "Plumber", "plumber", true)),
    ServiceCategoryOption(R.string.cat_carpenter, Category("cat_carpenter", "Carpenter", "carpenter", true)),
    ServiceCategoryOption(R.string.cat_painter, Category("cat_painter", "Painter", "painter", true)),
    ServiceCategoryOption(R.string.cat_mason, Category("cat_mason", "Mason", "mason", true)),
    ServiceCategoryOption(R.string.cat_roofing, Category("cat_roofing", "Roofing", "roofing", true)),
    ServiceCategoryOption(R.string.cat_ac_repair, Category("cat_ac_repair", "AC Repair", "ac_repair", true)),
    ServiceCategoryOption(R.string.cat_glazier, Category("cat_glazier", "Glazier", "glazier", true)),
    ServiceCategoryOption(R.string.cat_cook, Category("cat_cook", "Cook", "cook", true)),
    ServiceCategoryOption(R.string.cat_babysitter, Category("cat_babysitter", "Babysitter", "babysitter", true)),
    ServiceCategoryOption(R.string.cat_nurse, Category("cat_home_nurse", "Home Nurse", "nurse", true)),
    ServiceCategoryOption(R.string.cat_car_wash, Category("cat_car_wash", "Car Wash", "car_wash", true)),
    ServiceCategoryOption(R.string.cat_moving, Category("cat_moving", "Moving", "moving", true)),
    ServiceCategoryOption(R.string.cat_gardener, Category("cat_gardener", "Gardener", "gardener", true)),
    ServiceCategoryOption(R.string.cat_car_repair, Category("cat_mechanic", "Mechanic", "mechanic", true)),
    ServiceCategoryOption(R.string.cat_delivery, Category("cat_delivery", "Delivery", "delivery", true)),
    ServiceCategoryOption(R.string.cat_errands, Category("cat_errands", "Errands", "errands", true)),
    ServiceCategoryOption(R.string.cat_pest_control, Category("cat_pest_control", "Pest Control", "cockroach", true)),
    ServiceCategoryOption(R.string.cat_locksmith, Category("cat_locksmith", "Locksmith", "locksmith", true)),
    ServiceCategoryOption(R.string.cat_appliance_repair, Category("cat_appliance_repair", "Appliance Repair", "appliance_repair", true)),
    ServiceCategoryOption(R.string.cat_flooring_tiling, Category("cat_flooring_tiling", "Flooring / Tiling", "flooring", true)),
    ServiceCategoryOption(R.string.cat_interior_design, Category("cat_interior_design", "Interior Design", "interior_design", true)),
    ServiceCategoryOption(R.string.cat_welding_ironwork, Category("cat_welding_ironwork", "Welding / Ironwork", "ironworker", true)),
    ServiceCategoryOption(R.string.cat_satellite_tv_installation, Category("cat_satellite_tv_installation", "Satellite / TV Installation", "satelite_dish", true)),
    ServiceCategoryOption(R.string.cat_barber_haircut_at_home, Category("cat_barber_haircut_home", "Barber / Haircut at Home", "barber", true)),
    ServiceCategoryOption(R.string.cat_beauty_makeup, Category("cat_beauty_makeup", "Beauty & Makeup", "beauty_makeup", true)),
    ServiceCategoryOption(R.string.cat_personal_trainer, Category("cat_personal_trainer", "Personal Trainer", "trainer", true)),
    ServiceCategoryOption(R.string.cat_photographer_videographer, Category("cat_photographer_videographer", "Photographer / Videographer", "photographer", true)),
    ServiceCategoryOption(R.string.cat_tutoring_private_teacher, Category("cat_tutoring_private_teacher", "Tutoring / Private Teacher", "teacher", true)),
    ServiceCategoryOption(R.string.cat_it_tech_support, Category("cat_it_tech_support", "IT & Tech Support", "it_support", true)),
    ServiceCategoryOption(R.string.cat_veterinarian, Category("cat_veterinarian", "Veterinarian", "veterinary", true))
)

val defaultServiceCategories = serviceCategoryOptions.map { it.category }
