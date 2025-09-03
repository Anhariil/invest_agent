<%-- chart.jsp --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Круговая диаграмма</title>
    <!-- Подключаем Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
    <div style="width: 500px; height: 500px;">
        <canvas id="pieChart"></canvas>
    </div>

    <script>
        // Получаем данные из модели Spring
        const labels = JSON.parse('${labels}');
        const values = JSON.parse('${values}');
        const colors = JSON.parse('${colors}');

        // Создаем диаграмму
        const ctx = document.getElementById('pieChart').getContext('2d');
        new Chart(ctx, {
            type: 'pie',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors,
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'top'
                    }
                }
            }
        });
    </script>
</body>
</html>