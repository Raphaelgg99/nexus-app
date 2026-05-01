import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, switchMap } from 'rxjs';

import {
  FolderMockup,
  FolderResponse,
  MockupResponse,
} from '../../services/folder-mockup';
import { GeneratedPrototype, Prototype } from '../../services/prototype';

type SystemProductOption = MockupResponse & {
  folderName: string;
  fileName: string;
};

@Component({
  selector: 'app-image-generator',
  imports: [CommonModule, FormsModule],
  templateUrl: './image-generator.html',
  styleUrl: './image-generator.css',
})
export class ImageGenerator implements OnDestroy {
  @ViewChild('referenceUploadInput')
  private referenceUploadInput?: ElementRef<HTMLInputElement>;

  readonly maxImages = 5;
  prompt = '';
  private improvedPrompt: string | null = null;
  private improvedPromptSource = '';
  selectedFiles: File[] = [];
  previewUrls: string[] = [];
  generatedImageUrl: string | null = null;
  generationResult: GeneratedPrototype | null = null;
  isSubmitting = false;
  isImprovingPrompt = false;
  isDragActive = false;
  errorMessage = '';
  successMessage = '';
  debugMessage = '';
  isImageSourceModalOpen = false;
  isSystemLibraryOpen = false;
  isLoadingFolders = false;
  isLoadingSystemImages = false;
  systemSourceError = '';
  availableFolders: FolderResponse[] = [];
  selectedSystemFolderId: number | null = null;
  availableSystemProducts: SystemProductOption[] = [];
  selectedSystemProductIds: number[] = [];
  openProductMenuId: number | null = null;
  isProcessingSystemProduct = false;
  private readonly systemProductCache = new Map<number, SystemProductOption[]>();

  constructor(
    private readonly prototypeService: Prototype,
    private readonly folderMockupService: FolderMockup,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnDestroy(): void {
    this.revokePreviewUrls();
  }

  get isSecretModeActive(): boolean {
    return !!this.prompt.trim() && this.improvedPromptSource === this.prompt.trim();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];

    this.setSelectedFiles(files);
    input.value = '';
  }

  onPromptChange(value: string): void {
    this.prompt = value;

    if (value.trim() !== this.improvedPromptSource) {
      this.improvedPrompt = null;
      this.improvedPromptSource = '';
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragActive = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragActive = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragActive = false;

    const files = event.dataTransfer?.files
      ? Array.from(event.dataTransfer.files)
      : [];

    this.setSelectedFiles(files);
  }

  openImageSourceModal(): void {
    if (this.selectedFiles.length >= this.maxImages) {
      this.errorMessage = `Voce pode enviar no maximo ${this.maxImages} imagens por vez.`;
      return;
    }

    this.systemSourceError = '';
    this.isImageSourceModalOpen = true;
    this.isSystemLibraryOpen = false;
  }

  closeImageSourceModal(): void {
    this.isImageSourceModalOpen = false;
    this.isSystemLibraryOpen = false;
    this.systemSourceError = '';
    this.selectedSystemProductIds = [];
    this.openProductMenuId = null;
  }

  chooseComputerImages(): void {
    this.isImageSourceModalOpen = false;
    this.isSystemLibraryOpen = false;
    this.referenceUploadInput?.nativeElement.click();
  }

  chooseSystemImages(): void {
    this.isSystemLibraryOpen = true;
    this.systemSourceError = '';

    if (!this.availableFolders.length) {
      this.loadSystemFolders();
      return;
    }

    if (this.selectedSystemFolderId === null && this.availableFolders.length) {
      this.selectSystemFolder(this.availableFolders[0].id);
    }
  }

  loadSystemFolders(): void {
    this.isLoadingFolders = true;
    this.systemSourceError = '';

    this.folderMockupService
      .findAllFolders()
      .pipe(
        finalize(() => {
          this.isLoadingFolders = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (folders) => {
          this.availableFolders = folders;

          if (!folders.length) {
            this.selectedSystemFolderId = null;
            this.availableSystemProducts = [];
            this.systemSourceError = 'Nenhuma pasta com produtos foi encontrada no sistema.';
            return;
          }

          const preferredFolderId = this.selectedSystemFolderId ?? folders[0].id;
          this.selectSystemFolder(preferredFolderId);
        },
        error: () => {
          this.systemSourceError = 'Nao foi possivel carregar as imagens salvas no sistema.';
        },
      });
  }

  selectSystemFolder(folderId: number, forceReload = false): void {
    if (
      !forceReload &&
      this.selectedSystemFolderId === folderId &&
      this.availableSystemProducts.length
    ) {
      return;
    }

    this.selectedSystemFolderId = folderId;
    this.selectedSystemProductIds = [];
    this.openProductMenuId = null;
    this.systemSourceError = '';

    if (!forceReload) {
      const cachedProducts = this.systemProductCache.get(folderId);

      if (cachedProducts) {
        this.availableSystemProducts = cachedProducts;

        if (!cachedProducts.length) {
          this.systemSourceError = 'Essa pasta ainda nao tem produtos salvos.';
        }

        this.cdr.detectChanges();
        return;
      }
    }

    this.isLoadingSystemImages = true;

    this.folderMockupService
      .findMockupsByFolderId(folderId)
      .pipe(
        finalize(() => {
          this.isLoadingSystemImages = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (mockups) => {
          const activeFolder =
            this.availableFolders.find((folder) => folder.id === folderId)?.name ?? 'Pasta';

          this.availableSystemProducts = mockups.map((product, index) => ({
            ...product,
            folderName: activeFolder,
            fileName: this.buildSystemFileName(product, activeFolder, index),
          }));
          this.systemProductCache.set(folderId, this.availableSystemProducts);

          if (!mockups.length) {
            this.systemSourceError = 'Essa pasta ainda nao tem produtos salvos.';
          }
        },
        error: () => {
          this.availableSystemProducts = [];
          this.systemSourceError = 'Nao foi possivel carregar os produtos desta pasta.';
        },
      });
  }

  toggleSystemProductSelection(productId: number): void {
    this.selectedSystemProductIds = this.selectedSystemProductIds.includes(productId)
      ? this.selectedSystemProductIds.filter((id) => id !== productId)
      : [...this.selectedSystemProductIds, productId];
  }

  toggleProductMenu(productId: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    this.openProductMenuId = this.openProductMenuId === productId ? null : productId;
  }

  closeProductMenu(): void {
    this.openProductMenuId = null;
  }

  isProductMenuOpen(productId: number): boolean {
    return this.openProductMenuId === productId;
  }

  moveSystemProduct(product: SystemProductOption, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.isProcessingSystemProduct) {
      return;
    }

    const destinationOptions = this.availableFolders.filter(
      (folder) => folder.id !== product.folderId
    );

    if (!destinationOptions.length) {
      this.systemSourceError = 'Nao existe outra pasta disponivel para mover este produto.';
      this.closeProductMenu();
      return;
    }

    const destinationList = destinationOptions
      .map((folder, index) => `${index + 1}. ${folder.name}`)
      .join('\n');

    const choice = window.prompt(
      `Mover "${product.name || product.type}" para qual pasta?\n\n${destinationList}`
    );

    const selectedIndex = Number(choice) - 1;
    const destinationFolder = destinationOptions[selectedIndex];

    if (!destinationFolder) {
      this.closeProductMenu();
      return;
    }

    this.isProcessingSystemProduct = true;
    this.systemSourceError = '';
    this.closeProductMenu();

    this.folderMockupService
      .createMockup(destinationFolder.id, {
        image: product.image,
        name: product.name,
        type: product.name,
        description: product.description ?? undefined,
        sku: product.sku ?? undefined,
        available: product.available,
        stockQuantity: product.stockQuantity,
      })
      .pipe(
        switchMap(() =>
          this.folderMockupService.deleteMockup(product.folderId, product.id)
        ),
        finalize(() => {
          this.isProcessingSystemProduct = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.selectedSystemProductIds = this.selectedSystemProductIds.filter(
            (id) => id !== product.id
          );
          this.systemProductCache.delete(product.folderId);
          this.systemProductCache.delete(destinationFolder.id);
          this.selectSystemFolder(product.folderId, true);
        },
        error: () => {
          this.systemSourceError = 'Nao foi possivel mover o produto para outra pasta.';
        },
      });
  }

  deleteSystemProduct(product: SystemProductOption, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    if (this.isProcessingSystemProduct) {
      return;
    }

    const shouldDelete = window.confirm(`Excluir o produto "${product.name}" desta pasta?`);

    if (!shouldDelete) {
      this.closeProductMenu();
      return;
    }

    this.isProcessingSystemProduct = true;
    this.systemSourceError = '';
    this.closeProductMenu();

    this.folderMockupService
      .deleteMockup(product.folderId, product.id)
      .pipe(
        finalize(() => {
          this.isProcessingSystemProduct = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: () => {
          this.selectedSystemProductIds = this.selectedSystemProductIds.filter(
            (id) => id !== product.id
          );
          this.systemProductCache.delete(product.folderId);
          this.selectSystemFolder(product.folderId, true);
        },
        error: () => {
          this.systemSourceError = 'Nao foi possivel excluir o produto agora.';
        },
      });
  }

  addSelectedSystemImages(): void {
    const selectedProducts = this.availableSystemProducts.filter((product) =>
      this.selectedSystemProductIds.includes(product.id)
    );

    if (!selectedProducts.length) {
      this.systemSourceError = 'Selecione pelo menos um produto do sistema.';
      return;
    }

    const files = selectedProducts.map((product) =>
      this.dataUrlToFile(product.image, product.fileName, product.id)
    );

    this.setSelectedFiles(files);
    this.closeImageSourceModal();
  }

  isSystemProductSelected(productId: number): boolean {
    return this.selectedSystemProductIds.includes(productId);
  }

  clearPrompt(): void {
    this.prompt = '';
    this.improvedPrompt = null;
    this.improvedPromptSource = '';
  }

  removeReference(): void {
    this.selectedFiles = [];
    this.revokePreviewUrls();
    this.errorMessage = '';
  }

  removeReferenceAt(index: number, event: Event): void {
    event.preventDefault();
    event.stopPropagation();

    const previewUrl = this.previewUrls[index];
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    this.selectedFiles = this.selectedFiles.filter((_, fileIndex) => fileIndex !== index);
    this.previewUrls = this.previewUrls.filter((_, previewIndex) => previewIndex !== index);
    this.errorMessage = '';
    this.successMessage = '';
    this.debugMessage = '';
  }

  downloadGeneratedImage(): void {
    if (!this.generatedImageUrl) {
      return;
    }

    const link = document.createElement('a');
    link.href = this.generatedImageUrl;
    link.download = `ramark-gerada-${Date.now()}.png`;
    link.click();
  }

  improvePrompt(): void {
    if (this.isImprovingPrompt || this.isSubmitting) {
      return;
    }

    const trimmedPrompt = this.prompt.trim();
    if (!trimmedPrompt) {
      this.errorMessage = 'Escreva um prompt antes de usar o aperfeicoamento.';
      this.successMessage = '';
      return;
    }

    this.isImprovingPrompt = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.debugMessage = 'Ativando o modo secreto de aperfeicoamento...';

    this.prototypeService
      .improvePrompt(trimmedPrompt)
      .pipe(
        finalize(() => {
          this.isImprovingPrompt = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (result) => {
          this.improvedPrompt = result.prompt;
          this.improvedPromptSource = trimmedPrompt;
          this.successMessage = 'Modo de aperfeicoamento ativado com sucesso.';
          this.debugMessage = 'Ajustes avancados aplicados para elevar a qualidade da geracao.';
          this.cdr.detectChanges();
        },
        error: (error) => {
          const message = this.resolveGeneratorErrorMessage(
            error,
            'Nao foi possivel aperfeicoar o prompt agora.'
          );

          this.errorMessage = message;
          this.debugMessage = 'O aperfeicoamento do prompt falhou.';
          console.error('Prompt improvement error:', error);
          this.cdr.detectChanges();
        },
      });
  }

  generateImage(): void {
    if (this.isSubmitting) {
      return;
    }

    if (!this.selectedFiles.length) {
      this.errorMessage = 'Selecione uma imagem de referencia antes de gerar.';
      this.successMessage = '';
      return;
    }

    const trimmedPrompt = this.prompt.trim();
    if (!trimmedPrompt) {
      this.errorMessage = 'Descreva no prompt o que voce deseja gerar.';
      this.successMessage = '';
      return;
    }

    const promptToGenerate =
      this.improvedPrompt && this.improvedPromptSource === trimmedPrompt
        ? this.improvedPrompt
        : trimmedPrompt;

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.debugMessage = 'Enviando requisicao para o gerador...';

    this.prototypeService
      .generate(this.selectedFiles, promptToGenerate)
      .pipe(
        finalize(() => {
          this.isSubmitting = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (result) => {
          this.isSubmitting = false;
          this.generationResult = result;
          this.generatedImageUrl = result.imageUrl;
          this.successMessage = 'Imagem gerada com sucesso.';
          this.debugMessage = `Resposta recebida. imageUrl com ${result.imageUrl?.length ?? 0} caracteres.`;
          console.log('Image generator response:', result);
          this.cdr.detectChanges();
        },
        error: (error) => {
          this.isSubmitting = false;
          const message = this.resolveGeneratorErrorMessage(
            error,
            'Nao foi possivel gerar a imagem agora.'
          );

          this.errorMessage = message;
          this.generatedImageUrl = null;
          this.debugMessage = 'A requisicao retornou erro no front.';
          console.error('Image generator error:', error);
          this.cdr.detectChanges();
        },
      });
  }

  private setSelectedFiles(files: File[]): void {
    const validFiles = files.filter((file) => file.type.startsWith('image/'));

    if (!validFiles.length) {
      return;
    }

    const existingKeys = new Set(
      this.selectedFiles.map((file) => `${file.name}-${file.size}-${file.lastModified}`)
    );

    const freshFiles = validFiles.filter((file) => {
      const key = `${file.name}-${file.size}-${file.lastModified}`;
      return !existingKeys.has(key);
    });

    if (!freshFiles.length) {
      return;
    }

    const availableSlots = this.maxImages - this.selectedFiles.length;

    if (availableSlots <= 0) {
      this.errorMessage = `Voce pode enviar no maximo ${this.maxImages} imagens por vez.`;
      return;
    }

    const filesToAdd = freshFiles.slice(0, availableSlots);

    this.selectedFiles = [...this.selectedFiles, ...filesToAdd];
    this.previewUrls = [
      ...this.previewUrls,
      ...filesToAdd.map((file) => URL.createObjectURL(file)),
    ];
    this.errorMessage =
      filesToAdd.length < freshFiles.length
        ? `Limite de ${this.maxImages} imagens atingido.`
        : '';
    this.successMessage = '';
    this.debugMessage = '';
  }

  private revokePreviewUrls(): void {
    for (const previewUrl of this.previewUrls) {
      URL.revokeObjectURL(previewUrl);
    }

    this.previewUrls = [];
  }

  private buildSystemFileName(
    product: MockupResponse,
    folderName: string,
    index: number
  ): string {
    const baseName = `${folderName}-${product.name || product.type || `produto-${index + 1}`}`
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');

    const mimeType = this.extractMimeType(product.image);
    const extension = mimeType.split('/')[1] || 'png';

    return `${baseName || 'produto'}-${product.id}.${extension}`;
  }

  private extractMimeType(dataUrl: string): string {
    const match = dataUrl.match(/^data:(image\/[a-zA-Z0-9.+-]+);base64,/);
    return match?.[1] ?? 'image/png';
  }

  private dataUrlToFile(dataUrl: string, fileName: string, lastModified = Date.now()): File {
    const [header, content] = dataUrl.split(',');
    const mimeType = this.extractMimeType(dataUrl);
    const byteCharacters = atob(content ?? '');
    const byteNumbers = new Array(byteCharacters.length);

    for (let index = 0; index < byteCharacters.length; index += 1) {
      byteNumbers[index] = byteCharacters.charCodeAt(index);
    }

    return new File([new Uint8Array(byteNumbers)], fileName, {
      type: mimeType,
      lastModified,
    });
  }

  private resolveGeneratorErrorMessage(error: any, fallbackMessage: string): string {
    const backendMessages = error?.error?.messages;

    if (Array.isArray(backendMessages) && backendMessages.length) {
      return backendMessages[0];
    }

    if (typeof error?.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }

    if (error?.status === 401) {
      return 'Sua sessao nao foi aceita para esta operacao. Faca login novamente se isso continuar.';
    }

    if (error?.status === 403) {
      return 'Seu usuario nao tem permissao para executar esta operacao.';
    }

    if (typeof error?.message === 'string' && error.message.trim()) {
      return error.message;
    }

    return fallbackMessage;
  }
}
