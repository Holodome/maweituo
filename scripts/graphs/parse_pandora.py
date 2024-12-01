import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import yaml  # Не забудьте установить PyYAML, если его нет

# Загружаем конфигурацию из load.yaml
with open("../load.yaml", "r") as file:
    config = yaml.safe_load(file)

# Извлекаем порт из конфигурации
port = config['pools'][0]['gun']['target'].split(":")[1]  # Получаем порт, например, '8081'
filename = f"plot_{port}.png"  # Формируем имя файла для графика RPS и процентилей
histogram_filename = f"histogram_{port}.png"  # Формируем имя файла для гистограммы
data_filename = f"rps_response_times_{port}.csv"  # Формируем имя файла для данных

# Извлекаем значения from и to из конфигурации
rps_from = config['pools'][0]['rps']['from']
rps_to = config['pools'][0]['rps']['to']

# Загружаем данные
data = pd.read_csv(
    "../phout.log",
    sep='\t', names=[
        'time', 'tag', 'interval_real', 'connect_time', 'send_time',
        'latency', 'receive_time', 'interval_event', 'size_out',
        'size_in', 'net_code', 'proto_code'
    ]
)

# Фильтруем строки, не содержащие 'discarded' в столбце 'tag'
data = data[data['tag'] != 'discarded']

# Добавляем колонку с временем получения и округляем до секунд
data['ts'] = data.time + data.interval_real / 1000000
data['receive_sec'] = data.ts.astype(int)

# Индексируем по секундам и вычисляем время ответа в миллисекундах
data.set_index(['receive_sec'], inplace=True)
data.index = data.index - data.index.min()
data['rt_ms'] = data.interval_real / 1000


 


# Подсчитываем RPS и сохраняем в отдельный DataFrame
rps = data.groupby(level=0).count().time
rps.name = 'RPS'
print(rps)

# Вычисляем перцентили времени отклика по секундам
def percentile(n):
    def percentile_(x):
        return np.percentile(x, n)
    percentile_.__name__ = 'percentile_%s' % n
    return percentile_

percentiles = [percentile(n) for n in [99, 95, 90, 75, 50]]
response_percentiles = data.groupby(level=0).rt_ms.agg(percentiles)

# Построение графиков RPS и процентилей времени отклика
fig, ax1 = plt.subplots(figsize=(10, 6))

# График RPS
ax1.plot(rps.index, rps, color='blue', label='RPS')
ax1.set_xlabel('Time (seconds)')
ax1.set_ylabel('RPS (Requests per Second)', color='blue')
ax1.tick_params(axis='y', labelcolor='blue')

# Вторая ось для графика перцентилей времени отклика
ax2 = ax1.twinx()
response_percentiles.plot(ax=ax2, kind='line', linewidth=1)
ax2.set_ylabel('Response Time (ms)', color='gray')
ax2.tick_params(axis='y', labelcolor='gray')

# Заголовок и легенды
plt.title('RPS and Response Time Percentiles Over Time')
ax1.legend(loc='upper left')
ax2.legend(loc='upper left')  # Легенда процентилей слева сверху

# Сохранение графика RPS и процентилей
plt.savefig(filename)
plt.show()

# Группируем данные по значению RPS и строим гистограмму распределения времени отклика
plt.figure(figsize=(10, 6))
rps_intervals = np.linspace(rps_from, rps_to, 21)  # Делаем 20 интервалов от rps_from до rps_to
data['rps'] = rps.reindex(data.index, method='ffill')  # Добавляем столбец RPS ко всем строкам по времени

# Считаем распределение времени отклика для каждого интервала RPS
data_rps_intervals = pd.cut(data['rps'], bins=rps_intervals)
means = data.groupby(data_rps_intervals)['rt_ms'].mean()

# Сохранение средних значений времени отклика в CSV файл
means_df = pd.DataFrame({
    'RPS Range': [f"{int(interval.left)} - {int(interval.right)}" for interval in data_rps_intervals.cat.categories],
    'Average Response Time (ms)': means.values
})
means_df.to_csv(data_filename, index=False)

# Построение гистограммы с подписями крайних значений интервалов
plt.bar(range(len(means)), means, color='green', edgecolor='black')
plt.xticks(range(len(means)), [f"{int(interval.left)} - {int(interval.right)}" for interval in data_rps_intervals.cat.categories], rotation=30)

# Настройка осей
plt.xlabel('RPS Range')
plt.ylabel('Average Response Time (ms)')
plt.title('Average Response Time in RPS Ranges')

# Сохранение гистограммы
plt.savefig(histogram_filename)
plt.show()
