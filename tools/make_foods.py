"""Builds app/src/main/assets/foods.json, the app's built-in food list.

Each row: name, protein, kcal, carbs, fat (all per 100 g, or per 100 ml when ml=True),
portions [(label, grams)], other names for search, estimate flag (home-cooked dishes vary).
Values are approximate. Replace this list later with your own sheet.
"""
import json
import os

E = True  # estimate
F = []


def add(name, p, k, c, f, portions=(), aka="", est=False, ml=False):
    F.append({
        "n": name, "p": p, "k": k, "c": c, "f": f,
        "u": [[a, b] for a, b in portions],
        "a": [x.strip() for x in aka.split(",") if x.strip()],
        "e": est, "ml": ml,
    })


# Dairy
add("Milk, whole", 3.2, 61, 4.8, 3.3, [("glass (250 ml)", 250)], "doodh, full cream milk", ml=True)
add("Milk, toned", 3.3, 58, 4.9, 3.0, [("glass (250 ml)", 250)], "doodh", ml=True)
add("Milk, double toned", 3.3, 46, 5.0, 1.5, [("glass (250 ml)", 250)], "doodh, low fat milk", ml=True)
add("Milk, skimmed", 3.4, 34, 5.0, 0.1, [("glass (250 ml)", 250)], "skim milk, fat free milk", ml=True)
add("Curd", 3.5, 61, 4.7, 3.3, [("bowl (200 g)", 200)], "dahi, yogurt, yoghurt")
add("Greek yogurt", 10.0, 59, 3.6, 0.4, [("cup (170 g)", 170)], "hung curd, greek yoghurt")
add("Paneer", 18.0, 265, 1.2, 21.0, [("cube (20 g)", 20)], "panir, cottage cheese")
add("Paneer, low fat", 24.0, 200, 3.0, 9.0, [("cube (20 g)", 20)], "lite paneer, panir", est=E)
add("Cheese, cheddar", 25.0, 403, 1.3, 33.0, [("slice (20 g)", 20)], "cheese")
add("Cheese slice", 18.0, 300, 7.0, 24.0, [("slice (20 g)", 20)], "processed cheese")
add("Cottage cheese", 11.0, 98, 3.4, 4.3, [("cup (210 g)", 210)], "")
add("Butter", 0.9, 717, 0.1, 81.0, [("tsp (5 g)", 5)], "makhan")
add("Ghee", 0.0, 900, 0.0, 100.0, [("tsp (5 g)", 5)], "clarified butter")
add("Buttermilk", 3.3, 40, 4.8, 0.9, [("glass (250 ml)", 250)], "chaas, chhach, mattha", ml=True)
add("Lassi, sweet", 2.8, 90, 14.0, 2.5, [("glass (250 ml)", 250)], "lassi", est=E, ml=True)
add("Whey protein", 80.0, 400, 8.0, 6.0, [("scoop (30 g)", 30)], "protein powder, whey")
add("Soy milk", 3.3, 54, 6.0, 1.8, [("glass (250 ml)", 250)], "soya milk", ml=True)
add("Almond milk, unsweetened", 0.4, 15, 0.6, 1.1, [("glass (250 ml)", 250)], "", ml=True)

# Eggs, meat, fish
add("Egg, boiled", 12.6, 155, 1.1, 10.6, [("egg (50 g)", 50)], "anda, eggs, whole egg")
add("Egg white", 10.9, 52, 0.7, 0.2, [("white (33 g)", 33)], "anda safedi, egg whites")
add("Omelette", 11.0, 154, 0.6, 12.0, [("2-egg omelette (120 g)", 120)], "omlet, anda", est=E)
add("Egg bhurji", 11.0, 180, 3.0, 14.0, [("bowl (120 g)", 120)], "scrambled eggs, anda bhurji", est=E)
add("Chicken breast, cooked", 31.0, 165, 0.0, 3.6, [("piece (120 g)", 120)], "chicken")
add("Chicken thigh, cooked", 26.0, 209, 0.0, 11.0, [("piece (100 g)", 100)], "chicken leg")
add("Chicken curry", 13.0, 160, 5.0, 9.0, [("bowl (200 g)", 200)], "chicken gravy, murgh", est=E)
add("Butter chicken", 12.0, 200, 6.0, 14.0, [("bowl (200 g)", 200)], "murgh makhani", est=E)
add("Chicken tikka", 24.0, 170, 3.0, 7.0, [("piece (30 g)", 30)], "", est=E)
add("Tandoori chicken", 25.0, 180, 2.0, 10.0, [("piece (100 g)", 100)], "", est=E)
add("Chicken biryani", 8.5, 180, 22.0, 6.0, [("plate (300 g)", 300)], "biriyani", est=E)
add("Chicken sausage", 15.0, 190, 4.0, 12.0, [("sausage (50 g)", 50)], "", est=E)
add("Keema", 15.0, 210, 4.0, 14.0, [("bowl (150 g)", 150)], "kheema, minced meat", est=E)
add("Seekh kebab", 17.0, 230, 4.0, 15.0, [("kebab (50 g)", 50)], "kabab", est=E)
add("Mutton, cooked", 25.0, 258, 0.0, 17.0, [("piece (50 g)", 50)], "lamb, goat meat")
add("Mutton curry", 14.0, 200, 4.0, 14.0, [("bowl (200 g)", 200)], "gosht, mutton gravy", est=E)
add("Fish curry", 12.0, 140, 4.0, 8.0, [("bowl (200 g)", 200)], "machli, macher jhol", est=E)
add("Fish, fried", 20.0, 230, 8.0, 13.0, [("piece (80 g)", 80)], "fish fry, machli", est=E)
add("Fish, steamed", 22.0, 130, 0.0, 4.5, [("piece (100 g)", 100)], "rohu, basa, fish", est=E)
add("Salmon, cooked", 22.0, 206, 0.0, 12.0, [("fillet (150 g)", 150)], "")
add("Tuna, canned in water", 25.5, 116, 0.0, 0.8, [("can (140 g)", 140)], "")
add("Prawns, cooked", 24.0, 99, 0.2, 0.3, [("bowl (100 g)", 100)], "shrimp, jhinga")

# Pulses, beans, soy
add("Dal, cooked", 9.0, 116, 20.0, 0.4, [("bowl (150 g)", 150)], "daal, lentils, masoor, toor, arhar", est=E)
add("Moong dal, cooked", 7.0, 105, 19.0, 0.4, [("bowl (150 g)", 150)], "mung dal, moong", est=E)
add("Chana dal, cooked", 8.5, 130, 22.0, 1.5, [("bowl (150 g)", 150)], "chana daal", est=E)
add("Dal makhani", 6.0, 150, 15.0, 7.0, [("bowl (150 g)", 150)], "maa ki dal", est=E)
add("Dal tadka", 6.5, 120, 15.0, 4.0, [("bowl (150 g)", 150)], "dal fry, daal", est=E)
add("Rajma, cooked", 8.7, 127, 22.8, 0.5, [("bowl (150 g)", 150)], "kidney beans, rajma chawal", est=E)
add("Chole, cooked", 8.9, 164, 27.0, 2.6, [("bowl (150 g)", 150)], "chana masala, chickpeas, chhole", est=E)
add("Kala chana, boiled", 9.0, 155, 26.0, 2.7, [("bowl (100 g)", 100)], "black chickpeas, black chana")
add("Moong sprouts", 3.0, 30, 6.0, 0.2, [("bowl (100 g)", 100)], "sprouts, ankurit moong")
add("Roasted chana", 19.0, 360, 58.0, 6.0, [("handful (30 g)", 30)], "bhuna chana, roasted gram")
add("Soya chunks, dry", 52.0, 345, 33.0, 0.5, [("serving (30 g)", 30)], "nutrela, soybean chunks, meal maker")
add("Soya chunks, cooked", 17.0, 115, 11.0, 0.2, [("bowl (100 g)", 100)], "nutrela, soya", est=E)
add("Tofu, firm", 17.3, 144, 2.8, 8.7, [("cube (25 g)", 25)], "soya paneer, bean curd")
add("Tofu, silken", 4.8, 55, 2.9, 2.7, [("bowl (150 g)", 150)], "soft tofu")
add("Tempeh", 20.0, 192, 7.6, 11.0, [("serving (100 g)", 100)], "")
add("Edamame", 11.9, 121, 8.9, 5.2, [("bowl (100 g)", 100)], "green soybeans")
add("Green peas, cooked", 5.4, 84, 15.6, 0.2, [("bowl (100 g)", 100)], "matar")
add("Black beans, cooked", 8.9, 132, 23.7, 0.5, [("bowl (150 g)", 150)], "")
add("Sambar", 3.0, 65, 9.0, 2.0, [("bowl (200 g)", 200)], "sambhar", est=E)
add("Kadhi", 3.0, 80, 7.0, 4.5, [("bowl (200 g)", 200)], "kadi, kadhi pakora", est=E)
add("Hummus", 7.9, 166, 14.0, 9.6, [("tbsp (30 g)", 30)], "")

# Grains and breakfast
add("Roti", 9.9, 297, 46.0, 7.5, [("roti (40 g)", 40)], "chapati, phulka, rotli", est=E)
add("Paratha, plain", 7.0, 320, 45.0, 13.0, [("paratha (60 g)", 60)], "parantha", est=E)
add("Aloo paratha", 5.5, 250, 35.0, 10.0, [("paratha (120 g)", 120)], "aloo parantha", est=E)
add("Naan", 9.0, 290, 50.0, 6.0, [("naan (90 g)", 90)], "nan, butter naan", est=E)
add("Rice, cooked", 2.7, 130, 28.0, 0.3, [("bowl (150 g)", 150)], "chawal, white rice")
add("Brown rice, cooked", 2.6, 112, 23.0, 0.9, [("bowl (150 g)", 150)], "")
add("Jeera rice", 3.0, 145, 28.0, 2.5, [("bowl (150 g)", 150)], "", est=E)
add("Veg biryani", 4.0, 165, 26.0, 5.0, [("plate (300 g)", 300)], "biriyani, vegetable biryani", est=E)
add("Veg pulao", 3.5, 150, 25.0, 4.0, [("plate (250 g)", 250)], "pulav", est=E)
add("Khichdi", 4.5, 120, 20.0, 2.5, [("bowl (250 g)", 250)], "khichri", est=E)
add("Poha", 2.6, 130, 25.0, 2.5, [("plate (150 g)", 150)], "pohe, flattened rice", est=E)
add("Upma", 3.0, 120, 18.0, 4.0, [("plate (200 g)", 200)], "rava upma", est=E)
add("Idli", 4.0, 140, 28.0, 0.5, [("idli (40 g)", 40)], "idly", est=E)
add("Dosa, plain", 4.0, 165, 27.0, 4.0, [("dosa (80 g)", 80)], "dosai", est=E)
add("Masala dosa", 3.8, 175, 26.0, 6.0, [("dosa (150 g)", 150)], "dosai", est=E)
add("Uttapam", 4.5, 155, 25.0, 4.0, [("uttapam (120 g)", 120)], "uthappam", est=E)
add("Medu vada", 6.0, 300, 30.0, 17.0, [("vada (50 g)", 50)], "vada, wada", est=E)
add("Dhokla", 6.0, 160, 25.0, 4.0, [("piece (30 g)", 30)], "khaman", est=E)
add("Besan chilla", 9.0, 190, 20.0, 8.0, [("chilla (80 g)", 80)], "cheela, gram flour pancake", est=E)
add("Oats", 13.0, 380, 67.0, 6.5, [("serving (40 g)", 40)], "rolled oats, oatmeal")
add("Muesli", 10.0, 370, 66.0, 6.0, [("bowl (50 g)", 50)], "granola")
add("Cornflakes", 7.0, 357, 84.0, 0.4, [("bowl (30 g)", 30)], "corn flakes, cereal")
add("Bread, white", 9.0, 265, 49.0, 3.2, [("slice (25 g)", 25)], "bread")
add("Bread, brown", 13.0, 247, 41.0, 3.4, [("slice (25 g)", 25)], "whole wheat bread, atta bread")
add("Pasta, cooked", 5.8, 158, 31.0, 0.9, [("plate (200 g)", 200)], "macaroni, spaghetti")
add("Instant noodles, dry", 9.0, 450, 61.0, 18.0, [("packet (70 g)", 70)], "maggi, noodles")
add("Quinoa, cooked", 4.4, 120, 21.0, 1.9, [("bowl (150 g)", 150)], "")
add("Daliya, cooked", 3.1, 83, 18.6, 0.2, [("bowl (200 g)", 200)], "dalia, broken wheat, bulgur")

# Vegetable dishes
add("Palak paneer", 7.0, 150, 6.0, 11.0, [("bowl (200 g)", 200)], "saag paneer", est=E)
add("Paneer butter masala", 8.0, 230, 9.0, 18.0, [("bowl (200 g)", 200)], "paneer makhani, shahi paneer", est=E)
add("Paneer tikka", 15.0, 240, 6.0, 17.0, [("piece (30 g)", 30)], "", est=E)
add("Matar paneer", 7.0, 160, 9.0, 10.0, [("bowl (200 g)", 200)], "mutter paneer", est=E)
add("Paneer bhurji", 14.0, 230, 5.0, 18.0, [("bowl (150 g)", 150)], "", est=E)
add("Aloo sabzi", 2.0, 105, 14.0, 5.0, [("bowl (150 g)", 150)], "potato curry, aloo", est=E)
add("Mixed veg sabzi", 2.5, 95, 10.0, 5.0, [("bowl (150 g)", 150)], "mix veg, sabji", est=E)
add("Aloo gobi", 2.5, 100, 11.0, 5.5, [("bowl (150 g)", 150)], "", est=E)
add("Bhindi sabzi", 2.3, 95, 9.0, 6.0, [("bowl (150 g)", 150)], "okra, ladyfinger", est=E)
add("Baingan bharta", 2.0, 85, 9.0, 5.0, [("bowl (150 g)", 150)], "brinjal, eggplant", est=E)
add("Pav bhaji", 4.0, 170, 24.0, 6.5, [("plate (300 g)", 300)], "", est=E)
add("Chole bhature", 6.5, 270, 32.0, 13.0, [("plate (350 g)", 350)], "", est=E)

# Vegetables and fruit
add("Broccoli", 2.8, 34, 7.0, 0.4, [("cup (90 g)", 90)], "")
add("Spinach", 2.9, 23, 3.6, 0.4, [("cup (30 g)", 30)], "palak")
add("Mushrooms", 3.1, 22, 3.3, 0.3, [("cup (70 g)", 70)], "mushroom")
add("Potato, boiled", 1.9, 87, 20.0, 0.1, [("potato (150 g)", 150)], "aloo")
add("Sweet potato, boiled", 1.6, 86, 20.0, 0.1, [("piece (130 g)", 130)], "shakarkandi")
add("Sweet corn, boiled", 3.4, 96, 21.0, 1.5, [("cob (100 g)", 100)], "bhutta, corn")
add("Cauliflower", 1.9, 25, 5.0, 0.3, [("cup (100 g)", 100)], "gobi")
add("Cabbage", 1.3, 25, 6.0, 0.1, [("cup (90 g)", 90)], "patta gobi")
add("Carrot", 0.9, 41, 10.0, 0.2, [("carrot (60 g)", 60)], "gajar")
add("Cucumber", 0.7, 15, 3.6, 0.1, [("cucumber (200 g)", 200)], "kheera, kakdi")
add("Tomato", 0.9, 18, 3.9, 0.2, [("tomato (100 g)", 100)], "tamatar")
add("Onion", 1.1, 40, 9.0, 0.1, [("onion (100 g)", 100)], "pyaz, kanda")
add("Banana", 1.1, 89, 23.0, 0.3, [("banana (120 g)", 120)], "kela")
add("Apple", 0.3, 52, 14.0, 0.2, [("apple (180 g)", 180)], "seb")
add("Orange", 0.9, 47, 12.0, 0.1, [("orange (130 g)", 130)], "santra")
add("Mango", 0.8, 60, 15.0, 0.4, [("mango (200 g)", 200)], "aam")
add("Papaya", 0.5, 43, 11.0, 0.3, [("bowl (150 g)", 150)], "papita")
add("Grapes", 0.7, 69, 18.0, 0.2, [("bowl (100 g)", 100)], "angoor")
add("Watermelon", 0.6, 30, 7.6, 0.2, [("bowl (200 g)", 200)], "tarbooz")
add("Pomegranate", 1.7, 83, 19.0, 1.2, [("bowl (100 g)", 100)], "anar")
add("Guava", 2.6, 68, 14.0, 1.0, [("guava (100 g)", 100)], "amrood, peru")
add("Pineapple", 0.5, 50, 13.0, 0.1, [("bowl (150 g)", 150)], "ananas")
add("Dates", 1.8, 277, 75.0, 0.2, [("date (24 g)", 24)], "khajur")
add("Raisins", 3.0, 299, 79.0, 0.5, [("tbsp (10 g)", 10)], "kishmish")
add("Avocado", 2.0, 160, 8.5, 14.7, [("half (100 g)", 100)], "butter fruit")
add("Coconut, fresh", 3.3, 354, 15.0, 33.0, [("piece (40 g)", 40)], "nariyal")
add("Coconut water", 0.7, 19, 3.7, 0.2, [("glass (250 ml)", 250)], "nariyal pani", ml=True)

# Nuts, seeds, snacks
add("Peanuts, roasted", 24.0, 585, 21.0, 50.0, [("handful (30 g)", 30)], "moongfali, groundnuts")
add("Peanut butter", 25.0, 588, 20.0, 50.0, [("tbsp (16 g)", 16)], "")
add("Almonds", 21.0, 579, 22.0, 50.0, [("10 almonds (12 g)", 12)], "badam")
add("Cashews", 18.0, 553, 30.0, 44.0, [("10 cashews (15 g)", 15)], "kaju")
add("Walnuts", 15.0, 654, 14.0, 65.0, [("handful (30 g)", 30)], "akhrot")
add("Pistachios", 20.0, 560, 28.0, 45.0, [("handful (30 g)", 30)], "pista")
add("Chia seeds", 17.0, 486, 42.0, 31.0, [("tbsp (12 g)", 12)], "")
add("Flax seeds", 18.0, 534, 29.0, 42.0, [("tbsp (10 g)", 10)], "alsi, linseed")
add("Pumpkin seeds", 29.8, 559, 10.7, 49.0, [("tbsp (10 g)", 10)], "")
add("Sunflower seeds", 21.0, 584, 20.0, 51.0, [("tbsp (10 g)", 10)], "")
add("Makhana", 9.7, 347, 77.0, 0.1, [("bowl (30 g)", 30)], "fox nuts, lotus seeds")
add("Protein bar", 30.0, 380, 40.0, 12.0, [("bar (60 g)", 60)], "", est=E)
add("Popcorn, plain", 13.0, 387, 78.0, 4.5, [("bowl (25 g)", 25)], "")
add("Potato chips", 6.6, 536, 53.0, 34.0, [("packet (50 g)", 50)], "wafers, crisps")
add("Biscuits", 7.0, 440, 74.0, 13.0, [("biscuit (7 g)", 7)], "marie, cookies", est=E)
add("Dark chocolate", 7.8, 598, 46.0, 43.0, [("piece (10 g)", 10)], "chocolate")
add("Samosa", 4.5, 262, 31.0, 13.0, [("samosa (80 g)", 80)], "", est=E)
add("Pakora", 7.0, 280, 26.0, 16.0, [("piece (20 g)", 20)], "pakoda, bhajji", est=E)
add("Momos, veg", 5.0, 170, 26.0, 4.5, [("piece (25 g)", 25)], "momo, dumplings", est=E)
add("Momos, chicken", 9.0, 190, 22.0, 6.0, [("piece (25 g)", 25)], "momo, dumplings", est=E)

# Fast food and sweets
add("Pizza, cheese", 11.0, 266, 33.0, 10.0, [("slice (100 g)", 100)], "")
add("Veg burger", 6.0, 250, 30.0, 10.0, [("burger (150 g)", 150)], "", est=E)
add("Chicken burger", 13.0, 250, 24.0, 11.0, [("burger (160 g)", 160)], "", est=E)
add("French fries", 3.4, 312, 41.0, 15.0, [("medium (110 g)", 110)], "fries, chips")
add("Veg sandwich", 6.0, 200, 28.0, 7.0, [("sandwich (150 g)", 150)], "", est=E)
add("Gulab jamun", 4.0, 330, 50.0, 13.0, [("piece (40 g)", 40)], "", est=E)
add("Rasgulla", 4.0, 186, 40.0, 1.7, [("piece (50 g)", 50)], "rosogolla", est=E)
add("Kheer", 4.0, 140, 20.0, 5.0, [("bowl (150 g)", 150)], "payasam, rice pudding", est=E)
add("Besan ladoo", 7.0, 470, 52.0, 26.0, [("ladoo (30 g)", 30)], "laddu", est=E)
add("Ice cream, vanilla", 3.5, 207, 24.0, 11.0, [("scoop (65 g)", 65)], "icecream")

# Drinks and extras
add("Tea with milk", 1.0, 40, 6.0, 1.2, [("cup (150 ml)", 150)], "chai", est=E, ml=True)
add("Coffee with milk", 1.5, 40, 5.0, 1.5, [("cup (150 ml)", 150)], "coffee", est=E, ml=True)
add("Banana milkshake", 3.2, 100, 16.0, 3.0, [("glass (300 ml)", 300)], "shake", est=E, ml=True)
add("Orange juice", 0.7, 45, 10.4, 0.2, [("glass (250 ml)", 250)], "juice", ml=True)
add("Cola", 0.0, 42, 10.6, 0.0, [("can (330 ml)", 330)], "soft drink, soda", ml=True)
add("Honey", 0.3, 304, 82.0, 0.0, [("tbsp (21 g)", 21)], "shahad")
add("Sugar", 0.0, 387, 100.0, 0.0, [("tsp (4 g)", 4)], "cheeni, shakkar")
add("Olive oil", 0.0, 884, 0.0, 100.0, [("tbsp (14 g)", 14)], "oil")
add("Mayonnaise", 1.0, 680, 0.6, 75.0, [("tbsp (15 g)", 15)], "mayo")

out = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "foods.json")
os.makedirs(os.path.dirname(out), exist_ok=True)
with open(out, "w", encoding="utf-8") as fh:
    json.dump({"version": 1, "foods": F}, fh, ensure_ascii=False, separators=(",", ":"))
names = [x["n"] for x in F]
assert len(names) == len(set(names)), "duplicate names"
print(len(F), "foods written")
