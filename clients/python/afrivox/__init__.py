"""Client Python d'AfriVox (bibliothèque standard uniquement)."""

from .client import ApiError, Client, UnsafeUrlError, safe_extract, verify_archive

__all__ = ["Client", "ApiError", "UnsafeUrlError", "verify_archive", "safe_extract"]
__version__ = "0.1.0"
