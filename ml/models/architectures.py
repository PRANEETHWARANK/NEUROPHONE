"""
NeuroPhone PyTorch Neural Architectures
Mapping directly to Neural Networks & Deep Learning Syllabus:
- MLP (Multi-Layer Perceptron) with configurable Activations & Dropout
- CNN (1D Temporal Convolutional Network) for visual/spatial landmark feature extraction
- LSTM (Long Short-Term Memory) for temporal gesture dynamics
- GRU (Gated Recurrent Unit) for lightweight mobile sequence modeling
- Transformer (Multi-Head Attention Encoder) for long-sequence intent & context
- Autoencoder for behavioral anomaly detection & reconstruction error
"""

import math
import torch
import torch.nn as nn
import torch.nn.functional as F

class NeuroMLP(nn.Module):
    """Multi-Layer Perceptron for baseline intent & gesture classification."""
    def __init__(self, input_dim=1890, hidden_dim=128, num_classes=5, dropout_rate=0.2):
        super().__init__()
        self.fc1 = nn.Linear(input_dim, hidden_dim)
        self.bn1 = nn.BatchNorm1d(hidden_dim)
        self.relu = nn.ReLU()
        self.dropout = nn.Dropout(dropout_rate)
        self.fc2 = nn.Linear(hidden_dim, 64)
        self.bn2 = nn.BatchNorm1d(64)
        self.fc3 = nn.Linear(64, num_classes)

    def forward(self, x):
        if x.dim() > 2:
            x = x.view(x.size(0), -1)
        x = self.dropout(self.relu(self.bn1(self.fc1(x))))
        x = self.dropout(self.relu(self.bn2(self.fc2(x))))
        logits = self.fc3(x)
        return logits


class NeuroCNN(nn.Module):
    """1D Temporal CNN for spatial & temporal feature extraction across hand frames."""
    def __init__(self, in_channels=63, num_classes=5):
        super().__init__()
        self.conv1 = nn.Conv1d(in_channels, 64, kernel_size=3, padding=1)
        self.conv2 = nn.Conv1d(64, 128, kernel_size=3, padding=1)
        self.pool = nn.AdaptiveAvgPool1d(1)
        self.fc = nn.Linear(128, num_classes)

    def forward(self, x):
        # Input shape: [Batch, SeqLen, FeatDim] -> transpose to [Batch, FeatDim, SeqLen]
        if x.dim() == 3 and x.size(1) != 63:
            x = x.transpose(1, 2)
        x = F.relu(self.conv1(x))
        x = F.relu(self.conv2(x))
        x = self.pool(x).squeeze(-1)
        return self.fc(x)


class NeuroLSTM(nn.Module):
    """Recurrent LSTM for temporal hand gesture sequence modeling."""
    def __init__(self, input_dim=63, hidden_dim=64, num_layers=2, num_classes=5):
        super().__init__()
        self.lstm = nn.LSTM(input_dim, hidden_dim, num_layers=num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_dim, num_classes)

    def forward(self, x):
        out, (h_n, _) = self.lstm(x)
        # Use final time step hidden representation
        return self.fc(h_n[-1])


class NeuroGRU(nn.Module):
    """Lightweight GRU (Gated Recurrent Unit) optimized for mobile on-device inference."""
    def __init__(self, input_dim=63, hidden_dim=48, num_layers=1, num_classes=5):
        super().__init__()
        self.gru = nn.GRU(input_dim, hidden_dim, num_layers=num_layers, batch_first=True)
        self.fc = nn.Linear(hidden_dim, num_classes)

    def forward(self, x):
        out, h_n = self.gru(x)
        return self.fc(h_n[-1])


class PositionalEncoding(nn.Module):
    def __init__(self, d_model, max_len=100):
        super().__init__()
        pe = torch.zeros(max_len, d_model)
        position = torch.arange(0, max_len, dtype=torch.float).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, d_model, 2).float() * (-math.log(10000.0) / d_model))
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        self.register_buffer('pe', pe.unsqueeze(0))

    def forward(self, x):
        return x + self.pe[:, :x.size(1)]


class NeuroTransformer(nn.Module):
    """Lightweight Transformer Encoder with Multi-Head Attention for sequences."""
    def __init__(self, input_dim=63, d_model=64, nhead=4, num_layers=2, num_classes=5):
        super().__init__()
        self.input_proj = nn.Linear(input_dim, d_model)
        self.pos_encoder = PositionalEncoding(d_model)
        encoder_layer = nn.TransformerEncoderLayer(d_model=d_model, nhead=nhead, dim_feedforward=128, batch_first=True)
        self.transformer_encoder = nn.TransformerEncoder(encoder_layer, num_layers=num_layers)
        self.fc = nn.Linear(d_model, num_classes)

    def forward(self, x):
        x = self.input_proj(x)
        x = self.pos_encoder(x)
        out = self.transformer_encoder(x)
        # Global average pooling across sequence dimension
        pooled = out.mean(dim=1)
        return self.fc(pooled)


class NeuroAutoencoder(nn.Module):
    """Unsupervised Autoencoder for behavioral interaction anomaly detection."""
    def __init__(self, feature_dim=16, latent_dim=4):
        super().__init__()
        self.encoder = nn.Sequential(
            nn.Linear(feature_dim, 8),
            nn.ReLU(),
            nn.Linear(8, latent_dim),
            nn.ReLU()
        )
        self.decoder = nn.Sequential(
            nn.Linear(latent_dim, 8),
            nn.ReLU(),
            nn.Linear(8, feature_dim)
        )

    def forward(self, x):
        z = self.encoder(x)
        reconstruction = self.decoder(z)
        return reconstruction, z

    def compute_reconstruction_error(self, x):
        recon, _ = self.forward(x)
        return F.mse_loss(recon, x, reduction='none').mean(dim=-1)