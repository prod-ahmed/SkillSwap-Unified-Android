package com.skillswap.domain.skills

data class Skill(
    val name: String,
    val description: String? = null,
    val category: SkillCategory = SkillCategory.OTHER,
    val popularity: Int = 0
)

enum class SkillCategory {
    TECHNOLOGY,
    DESIGN,
    BUSINESS,
    LANGUAGE,
    ART,
    MUSIC,
    SPORT,
    COOKING,
    CRAFT,
    OTHER
}

// Predefined popular skills
object PredefinedSkills {
    val POPULAR_SKILLS = listOf(
        // Technology
        Skill("Python", "Programming language", SkillCategory.TECHNOLOGY, 100),
        Skill("JavaScript", "Programming language", SkillCategory.TECHNOLOGY, 95),
        Skill("React", "Frontend framework", SkillCategory.TECHNOLOGY, 90),
        Skill("Java", "Programming language", SkillCategory.TECHNOLOGY, 85),
        Skill("Kotlin", "Programming language", SkillCategory.TECHNOLOGY, 80),
        
        // Design
        Skill("Photoshop", "Image editing", SkillCategory.DESIGN, 100),
        Skill("Illustrator", "Vector graphics", SkillCategory.DESIGN, 90),
        Skill("Figma", "UI/UX design", SkillCategory.DESIGN, 85),
        Skill("UI Design", "User interface design", SkillCategory.DESIGN, 80),
        
        // Languages
        Skill("Anglais", "English language", SkillCategory.LANGUAGE, 100),
        Skill("Français", "French language", SkillCategory.LANGUAGE, 95),
        Skill("Espagnol", "Spanish language", SkillCategory.LANGUAGE, 85),
        Skill("Arabe", "Arabic language", SkillCategory.LANGUAGE, 80),
        Skill("Allemand", "German language", SkillCategory.LANGUAGE, 75),
        
        // Music
        Skill("Guitare", "Guitar", SkillCategory.MUSIC, 90),
        Skill("Piano", "Piano", SkillCategory.MUSIC, 85),
        Skill("Chant", "Singing", SkillCategory.MUSIC, 75),
        
        // Art
        Skill("Dessin", "Drawing", SkillCategory.ART, 80),
        Skill("Peinture", "Painting", SkillCategory.ART, 75),
        
        // Business
        Skill("Marketing", "Marketing", SkillCategory.BUSINESS, 85),
        Skill("Gestion de projet", "Project management", SkillCategory.BUSINESS, 80),
        Skill("Comptabilité", "Accounting", SkillCategory.BUSINESS, 70),
        
        // Sport
        Skill("Yoga", "Yoga", SkillCategory.SPORT, 85),
        Skill("Football", "Soccer", SkillCategory.SPORT, 90),
        Skill("Tennis", "Tennis", SkillCategory.SPORT, 75),
        
        // Cooking
        Skill("Cuisine italienne", "Italian cooking", SkillCategory.COOKING, 80),
        Skill("Pâtisserie", "Baking", SkillCategory.COOKING, 85),
        
        // Craft
        Skill("Couture", "Sewing", SkillCategory.CRAFT, 70),
        Skill("Menuiserie", "Woodworking", SkillCategory.CRAFT, 75)
    )
}
