Ваша задача - Реализовать с использованием графовой модели вычислений следующую задачу, разбив её на части и обеспечив максимальную потенциальную утилизацию ресурсов (изображение == матрица MxN):
Получив на вход изображения, найти на большем изображении координаты их вхождения в него, и вывести окрестность этого изображения.

Вам предоставлено 4 изображения и код, осуществляющий чтение этих изображений в матрицу пригодную для обработки. 
Для облегчения чтения и записи изображений, также даны бинарные файлы содержащие изображение в несжатом виде.
Формат файлов можно узнать из кода шаблона.

Задание должно состоять из одного файла flow.cpp содержащего логику создания графа, запуска и вывода на консоль необходимой информации.

Предлагается следующая структура графа:
    
    1. Узел принимающий путь к файлу с маленьким изображением и передающий матрицу изображения
    2. Узел принимающий матрицу и генерирующий из большого изображения все возможные прямоугольники размера искомого изображения
    3. Буферный узел
    4. Узел подсчитывающий разницу между искомым изображением и кандидатом
    5. Узел содержащий результат - минимальную разницу и координаты верхнего левого угла.
    6. Узел записывающий окрестность найденного изображения в файл.
    
Адекватные модификации шаблона, структуры графа или самого задания приветствуются.
Для каждого мелкого изображения на консоль должны выводиться его координаты.

Создаем проекты по аналогии с первой работой. Пожалуйста именуйте директории с задачей так же как и в первом задании.

Порядок сдачи работ:

1. Вы делаете форк репозитория
2. Создаете директорию со своими инициалами и решаете задачу
3. Делаете pull request в наш репозиторий.

При возникновении любых вопросов не стесняйтесь нам писать в Slack либо на почту.

Дедлайн - 20.05 23:59.
