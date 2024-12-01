# Открываем исходный файл для чтения и создаем новый файл для записи
with open('../db/data', 'r') as infile, open('../db/data0', 'w') as outfile:
    # Проходим по каждой строке в файле
    for line in infile:
        # Ищем строку с INSERT INTO и добавляем скобку после VALUES
        if "INSERT INTO public.users" in line and "VALUES" in line:
            # Добавляем скобку перед значениями, если её нет
            line = line.replace("VALUES", "VALUES (")
        # Записываем обработанную строку в новый файл
        outfile.write(line)
