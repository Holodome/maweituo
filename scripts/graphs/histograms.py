import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

histogram_filename = "combined_histogram.png"

# Загрузка данных из трех файлов
files = ["rps_response_times_8081.csv", "rps_response_times_8082.csv", "rps_response_times_8083.csv"]
dataframes = [pd.read_csv(file) for file in files]

# Подготовка данных для графика
bins = dataframes[0]['RPS Range']  # Предполагаем, что все файлы имеют одинаковые бины
average_response_times = [df['Average Response Time (ms)'].fillna(0) for df in dataframes]  # Заменяем NaN на 0

# Построение графика
x = np.arange(len(bins))  # Позиции для каждой группы бинов
width = 0.25  # Ширина каждого столбца

fig, ax = plt.subplots(figsize=(12, 6))

# Добавляем столбцы для каждого файла
colors = ['blue', 'green', 'orange']
labels = ['echo', 'gin', 'chi']

for i, avg_rt in enumerate(average_response_times):
    ax.bar(x + i * width, avg_rt, width, label=f"File {labels[i]}", color=colors[i])

# Настройка осей
ax.set_xlabel('RPS Range')
ax.set_ylabel('Average Response Time (ms)')
ax.set_title('Average Response Time per RPS Range for Each File')
ax.set_xticks(x + width)
ax.set_xticklabels(bins, rotation=45)
ax.legend()

# Отображение графика
plt.tight_layout()
plt.savefig(histogram_filename)
plt.show()
