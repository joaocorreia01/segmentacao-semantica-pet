# Segmentação Semântica de Pets — UNet + TFLite + Android

Atividade de Processamento Digital de Imagens: treinamento de um modelo de segmentação semântica (UNet) no dataset Oxford-IIIT Pet, exportação para TensorFlow Lite (LiteRT) e implantação em um aplicativo Android que realiza a inferência on-device e exibe a máscara sobreposta.

## Pipeline
1. Dataset: Oxford-IIIT Pet (torchvision), trimap remapeado para 3 classes — pet, fundo e borda.
2. Modelo: UNet treinada do zero em PyTorch, entrada/saída 128x128.
3. Métricas: IoU (Jaccard) e Acurácia via torchmetrics.
4. Exportação: PyTorch -> TFLite (FP32) com litert-torch.
5. Deploy: app Android em Kotlin com runtime LiteRT/TFLite (Interpreter), pré-processamento NCHW, argmax e overlay.

## Estrutura
- `colab/segmentacao_unet_pet.ipynb` — treino, avaliação e exportação.
- `colab/unet_pet.tflite` — modelo FP32, entrada/saída [1,3,128,128].
- `android/PetSeg/` — projeto Android Studio.
- `docs/` — print do app e visualização das predições.

## Como rodar o treino (Colab)
Abra `colab/segmentacao_unet_pet.ipynb` no Google Colab (runtime com GPU) e execute as células na ordem. A exportação requer reiniciar o runtime após instalar o litert-torch.

## Como rodar o app (Android)
1. Abra `android/PetSeg/` no Android Studio.
2. O modelo já está em `app/src/main/assets/unet_pet.tflite`.
3. Conecte um dispositivo e clique em Run.
4. No app: Selecionar imagem -> foto de um pet -> Rodar segmentação.

## Métricas de validação
| Métrica | Valor |
|---------|-------|
| IoU     | (preencher) |
| Acurácia| (preencher) |

## Stack
PyTorch · torchvision · torchmetrics · litert-torch · TensorFlow Lite (LiteRT) · Kotlin · Android